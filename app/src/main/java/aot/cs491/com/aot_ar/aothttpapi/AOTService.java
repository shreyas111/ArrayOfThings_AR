package aot.cs491.com.aot_ar.aothttpapi;

import android.util.Log;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import androidx.annotation.Nullable;
import aot.cs491.com.aot_ar.utils.DateDeserializer;
import aot.cs491.com.aot_ar.utils.DisposablesManager;
import aot.cs491.com.aot_ar.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class AOTService {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIME_ZONE = "UTC";

    private static final String TAG = AOTService.class.getSimpleName();

    private static final String BASE_URL = "https://api.arrayofthings.org/api/";
    private static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS);
    private static OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build();

    private static Gson gson = new GsonBuilder()
            .setLenient()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Date.class, new DateDeserializer(DATE_FORMAT, TimeZone.getTimeZone(TIME_ZONE)))
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    private AOTService() {
    }

    public static ArrayOfThingsAPI api() {
        return retrofit.create(ArrayOfThingsAPI.class);
    }

    public static String buildLocationString(double longitude, double latitude, int distanceInMeters) {
        return "distance:" + distanceInMeters + ":" + gson.toJson(Point.from(longitude, latitude));
    }

    public static String buildTimestampQuery(Date date) {
        if (date == null)
            return null;

        String dateString = Utils.dateToServerString(date);

        return dateString == null ? null : "ge:" + dateString;
    }

    public static Single<List<AOTNode>> fetchNodes(double longitude, double latitude, int distanceInMeters) {
        return api().getNodes(buildLocationString(longitude, latitude, distanceInMeters)).map(response -> response.data);
    }

    public static Single<List<AOTSensor>> fetchSensors(List<AOTNode> nodes, String ontology) {
        return api().getSensors(AOTNode.extractVSNs(nodes), ontology).map(response -> response.getData());
    }

    public static Single<List<AOTObservation>> fetchObservations(List<AOTNode> nodes, List<AOTSensor> sensors, Date startDate, Integer page, Integer pageSize) {
        return api().getObservations(AOTNode.extractVSNs(nodes), null, AOTSensor.extractPaths(sensors), null, buildTimestampQuery(startDate), null, "asc:timestamp", page, pageSize)
                .map(response -> response.getData());
    }

    public static Observable<AOTNode> fetchObservationsFromNearbyNodes(double longitude, double latitude, int distanceInMeters, Date startDate, @Nullable Date endDate) {
        return fetchObservationsFromNearbyNodes(longitude, latitude, distanceInMeters, startDate, endDate, 200, 500);
    }

    public static Observable<AOTNode> fetchObservationsFromNearbyNodes(double longitude, double latitude, int distanceInMeters, Date startDate, @Nullable Date endDate, int pages, int pageSize) {
        return fetchNodes(longitude, latitude, distanceInMeters)
                .toObservable()
                .flatMapIterable(nodes -> nodes)
                .flatMap(node -> Observable.create(emitter -> {
                            List<Observable<List<AOTObservation>>> calls = new ArrayList<>();
                            for (int i = 1; i <= pages; i++) {
                                List<AOTSensor> sensors = new ArrayList<>();
                                for(AOTSensorType sensorType: AOTSensorType.values()) {
                                    sensors.add(new AOTSensor(sensorType.toString()));
                                }
                                calls.add(fetchObservations(Arrays.asList(new AOTNode[]{node}), sensors, startDate, i, pageSize).toObservable());
                            }

                            DisposablesManager.add(
                                    Observable.fromIterable(calls)
                                            .concatMap(listSingle -> listSingle)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doOnNext(observations -> {
                                                if (node.getObservations() == null) {
                                                    node.setObservations(new ArrayList<>());
                                                }
                                                node.getObservations().addAll(observations);

                                                if (node.getSensors() == null) {
                                                    node.setSensors(new HashSet<>());
                                                }
                                                for (AOTObservation anObservation : observations) {
                                                    node.getSensors().add(new AOTSensor(anObservation.sensorPath));
                                                }
                                            })
                                            .takeWhile(observations -> !observations.isEmpty() && (endDate == null || observations.get(observations.size() - 1).timestamp.compareTo(endDate) <= 0))
                                            .doOnComplete(() -> {
                                                emitter.onNext(node);
                                                emitter.onComplete();
                                            })
                                            .subscribe(
                                                    observations -> {
                                                    },
                                                    throwable -> Log.e(TAG, "Error while fetching observations:", throwable)
                                            )
                            );
                        })
                );
    }

    public static Single<List<AOTObservation>> filterObservations(List<AOTObservation> observations, AOTSensorType sensorType, Date startTime, Date endTime) {
        return  Observable.fromIterable(observations)
                .filter(anObservation -> anObservation.sensorPath.toLowerCase().contains(sensorType.toString().toLowerCase()) && anObservation.timestamp.compareTo(startTime) >= 0 && (endTime == null || anObservation.timestamp.compareTo(endTime) <= 0))
                .toList()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());

        /*return Single.create(emitter -> {
            ArrayList<AOTObservation> filteredObservations = new ArrayList<>();

            for (AOTObservation anObservation : observations) {
                if (anObservation.sensorPath.toLowerCase().contains(sensorType.toString().toLowerCase()) && anObservation.timestamp.compareTo(startTime) >= 0 && (endTime == null || anObservation.timestamp.compareTo(endTime) <= 0)) {
                    filteredObservations.add(anObservation);
                }
            }
            emitter.onSuccess(filteredObservations);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());*/

//        return filteredObservations;
    }

    public static Single<AOTObservation> aggregateObservations(List<AOTObservation> observations, String aggregateFunction) {
        return Single.<AOTObservation>create(emitter -> {
            if (observations.isEmpty()) {
                // RxJava doesn't allow emitting null, so send blank object instead
                emitter.onSuccess(new AOTObservation());
                return;
            }

            AOTObservation aggregatedObservation = new AOTObservation();

            aggregatedObservation.setNodeVsn(observations.get(0).getNodeVsn());
            aggregatedObservation.setSensorPath((observations.get(0).getSensorPath()));
            aggregatedObservation.setTimestamp(observations.get(0).getTimestamp());

            Float aggregatedValue = Float.NaN;

            switch (aggregateFunction.toLowerCase()) {
                case "min":
                    aggregatedValue = Utils.minOfItems(observations, AOTObservation::getValue);
                    break;

                case "max":
                    aggregatedValue = Utils.maxOfItems(observations, AOTObservation::getValue);
                    break;

                case "sum":
                    aggregatedValue = Utils.sumItems(observations, AOTObservation::getValue);
                    break;

                case "avg":
                case "mean":
                    Float sum = Utils.sumItems(observations, AOTObservation::getValue);
                    if (!sum.isNaN()) {
                        aggregatedValue = sum / observations.size();
                    }
                    break;
            }

            aggregatedObservation.setValue(aggregatedValue);
            emitter.onSuccess(aggregatedObservation);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());

        /*if (observations.isEmpty())
            return null;

        AOTObservation aggregatedObservation = new AOTObservation();

        aggregatedObservation.setNodeVsn(observations.get(0).getNodeVsn());
        aggregatedObservation.setSensorPath((observations.get(0).getSensorPath()));
        aggregatedObservation.setTimestamp(observations.get(0).getTimestamp());

        Float aggregatedValue = Float.NaN;

        switch (aggregateFunction.toLowerCase()) {
            case "min":
                aggregatedValue = Utils.minOfItems(observations, AOTObservation::getValue);
                break;

            case "max":
                aggregatedValue = Utils.maxOfItems(observations, AOTObservation::getValue);
                break;

            case "sum":
                aggregatedValue = Utils.sumItems(observations, AOTObservation::getValue);
                break;

            case "avg":
            case "mean":
                Float sum = Utils.sumItems(observations, AOTObservation::getValue);
                if (!sum.isNaN()) {
                    aggregatedValue = sum / observations.size();
                }
                break;
        }

        aggregatedObservation.setValue(aggregatedValue);
        return aggregatedObservation;*/
    }
}

