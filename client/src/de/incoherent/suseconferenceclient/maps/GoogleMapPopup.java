/*******************************************************************************
 * Copyright (c) 2012 Matt Barringer <matt@incoherent.de>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Matt Barringer <matt@incoherent.de> - initial API and implementation
 ******************************************************************************/

package de.incoherent.suseconferenceclient.maps;



import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.incoherent.suseconferenceclient.R;

/*
 * The popup box for Google maps
 */

public class GoogleMapPopup {
	private View mView;
    private boolean mIsVisible = false;
    private MapView mMapView;

    public GoogleMapPopup(MapView mapView) {
    	this.mMapView = mapView;
    	ViewGroup parent=(ViewGroup) mapView.getParent();
        Context context = mapView.getContext();
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.map_bubble, parent, false);
    }
    
    public View getView() {
        return mView;
    }
    
    public void open(GoogleMapOverlayItem item) {
    	String name = item.getTitle();
    	String address = item.getAddress();
    	String description = item.getSnippet();
    	
    	TextView textView = (TextView) mView.findViewById(R.id.nameTextView);
    	textView.setText(name);
    	
    	textView = (TextView) mView.findViewById(R.id.addressTextView);
    	textView.setText(address);
    	
    	textView = (TextView) mView.findViewById(R.id.descriptionTextView);
    	textView.setText(description);
    	
        GeoPoint position = item.getPoint();
        MapView.LayoutParams lp = new MapView.LayoutParams(
                        MapView.LayoutParams.WRAP_CONTENT,
                        MapView.LayoutParams.WRAP_CONTENT,
                        position, MapView.LayoutParams.BOTTOM_CENTER);
        lp.mode = MapView.LayoutParams.MODE_MAP;
        close();
        
        mMapView.addView(mView, lp);
        mIsVisible = true;
    }
    
    public void close() {
        if (mIsVisible) {
                mIsVisible = false;
                ((ViewGroup) mView.getParent()).removeView(mView);
        }
    }

    public boolean isOpen(){
    	return mIsVisible;
    }

}
