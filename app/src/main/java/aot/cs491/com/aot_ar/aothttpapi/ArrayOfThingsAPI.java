package aot.cs491.com.aot_ar.aothttpapi;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ArrayOfThingsAPI {

    @GET("projects")
    Single<AOTResponse<List<AOTProject>>> getProjects();

    @GET("projects/{slug}")
    Single<AOTResponse<List<AOTProject>>> getProjectDetails(@Path("slug") String slug);

    @GET("nodes")
    Single<AOTResponse<List<AOTNode>>> getNodes(@Query("location") String location);

    @GET("nodes/{vsn}")
    Single<AOTResponse<List<AOTNode>>> getNodeDetails(@Path("vsn") String vsn);

    @GET("sensors")
    Single<AOTResponse<List<AOTSensor>>> getSensors(@Query("onboard_nodes[]") List<String> nodes, @Query("ontology") String ontology);

    @GET("sensors/{path}")
    Single<AOTResponse<List<AOTSensor>>> getSensoeDetails(@Path("path") String path);

    @GET("observations")
    Single<AOTResponse<List<AOTObservation>>> getObservations(@Query("from_nodes[]") List<String> nodes, @Query("embed_node") Boolean embedNode, @Query("by_sensors[]") List<String> sensors, @Query("location") String location, @Query("timestamp") String timestamp, @Query("value") String value, @Query("order") String order, @Query("page") Integer page, @Query("size") Integer size);

    @GET("raw-observations")
    Single<AOTResponse<List<AOTRawObservation>>> getRawObservations(@Query("from_nodes[]") List<String> nodes, @Query("embed_node") Boolean embedNode, @Query("by_sensors[]") List<String> sensors, @Query("location") String location, @Query("timestamp") String timestamp, @Query("value") String value, @Query("order") String order, @Query("page") Integer page, @Query("size") Integer size);
}
