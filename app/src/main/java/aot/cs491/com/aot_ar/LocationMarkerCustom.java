package aot.cs491.com.aot_ar;


import android.util.Log;

import com.google.ar.sceneform.Node;

import aot.cs491.com.aot_ar.aothttpapi.AOTNode;


public class LocationMarkerCustom extends uk.co.appoly.arcorelocation.LocationMarker {

    public AOTNode getAotNode() {
        return aotNode;
    }

    public void setAotNode(AOTNode aotNode) {
        this.aotNode = aotNode;
    }

    private AOTNode aotNode;
    public LocationMarkerCustom (double longitude, double latitude, Node node, AOTNode aotNode) {
        super(longitude, latitude, node);
        this.aotNode=aotNode;
        Log.d("Location Marker Custom", "Hello");
    }

}
