/**
 * 
 */
package de.incoherent.suseconferenceclient.tasks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.incoherent.suseconferenceclient.SUSEConferences;
import de.incoherent.suseconferenceclient.app.Database;
import de.incoherent.suseconferenceclient.app.HTTPWrapper;
import de.incoherent.suseconferenceclient.models.Conference;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;


public class CacheConferenceTask extends AsyncTask<Void, String, Long> {
	public interface CacheConferenceTaskListener {
		public void conferenceCached(long id, String message);
	}
	
	private Conference mConference;
	private Database db;
	private String mErrorMessage = "";
	private ProgressDialog mDialog = null;
	private CacheConferenceTaskListener mListener;
	
	public CacheConferenceTask(Context context, Conference conference, CacheConferenceTaskListener listener) {
		this.mConference = conference;
		this.db = SUSEConferences.getDatabase();
		this.mListener = listener;
		mDialog = ProgressDialog.show(context, "", 
				"Downloading data for " + mConference.getName(), true);
	}
	
	protected void onProgressUpdate(String... progress) {
		mDialog.setMessage("Loading " + progress[0]);
	}

	@Override
	protected Long doInBackground(Void... params) {
		Log.d("SUSEConferences", "Caching data from " + mConference.getUrl());
		String url = mConference.getUrl();
		String eventsUrl = url + "/events.json";
		String roomsUrl = url + "/rooms.json";
		String speakersUrl = url + "/speakers.json";
		String tracksUrl = url + "/tracks.json";
		String venueUrl = url + "/venue.json";
		Long returnVal = null;
		HashMap<String, Long> roomMap = new HashMap<String, Long>();
		HashMap<String, Long> trackMap = new HashMap<String, Long>();
		HashMap<String, Long> speakerMap = new HashMap<String, Long>();

		try {
			Log.d("SUSEConferences", "Venues: " + venueUrl);
			publishProgress("venues");
			JSONObject venueReply = HTTPWrapper.get(venueUrl);
			JSONObject venue = venueReply.getJSONObject("venue");
			String infoUrl = url + "/" + venue.getString("info_text");
			String info = HTTPWrapper.getRawText(infoUrl);
			String venueName = venue.getString("name");
			String venueAddr =  venue.getString("address");
			String offlineMap = "";
			String offlineMapBounds = "";
			if (venue.has("offline_map")) {
				offlineMap = venue.getString("offline_map");
				offlineMapBounds = venue.getString("offline_map_bounds");
			}

			long venueId = db.insertVenue(venue.getString("guid"),
					venueName,
					venueAddr,
					offlineMap,
					offlineMapBounds,
					info);
			JSONArray mapPoints = venue.getJSONArray("map_points");
			int mapLen = mapPoints.length();
			for (int i = 0; i < mapLen; i++) {
				JSONObject point = mapPoints.getJSONObject(i);
				String lat = point.getString("lat");
				String lon = point.getString("lon");
				String type = point.getString("type");
				String name = "Unknown Point";
				String addr = "Unknown Address";
				String desc = "";

				if (point.has("name")) {
					name = point.getString("name");
				}
				if (point.has("address")) {
					addr = point.getString("address");
				}
				if (point.has("description")) {
					desc = point.getString("description");
				}

				db.insertVenuePoint(venueId, lat, lon, type, name, addr, desc);
			}

			if (venue.has("map_polygons")) {
				JSONArray polygons = venue.getJSONArray("map_polygons");
				int polygonLen = polygons.length();
				for (int j = 0; j < polygonLen; j++) {
					JSONObject polygon = polygons.getJSONObject(j);
					String name = polygon.getString("name");
					String label = polygon.getString("label");
					String lineColorStr = polygon.getString("line_color");
					String fillColorStr = "#00000000";
					if (polygon.has("fill_color"))
						fillColorStr = polygon.getString("fill_color");

					List<String> stringList = new ArrayList<String>();
					JSONArray points = polygon.getJSONArray("points");
					int pointsLen = points.length();
					for (int k = 0; k < pointsLen; k++) {
						String newPoint = points.getString(k);
						stringList.add(newPoint);
					}
					String joined = TextUtils.join(";", stringList);	    				
					int lineColor = Color.parseColor(lineColorStr);
					int fillColor = Color.parseColor(fillColorStr);
					db.insertVenuePolygon(venueId, name, label, lineColor, fillColor, joined);
				}
			}

			db.setConferenceVenue(venueId, mConference.getSqlId());

			Log.d("SUSEConferences", "Rooms");
			publishProgress("rooms");
			JSONObject roomsReply = HTTPWrapper.get(roomsUrl);
			JSONArray rooms = roomsReply.getJSONArray("rooms");
			int roomsLen = rooms.length();
			for (int i = 0; i < roomsLen; i++) {
				JSONObject room = rooms.getJSONObject(i);
				String guid = room.getString("guid");
				Long roomId = db.insertRoom(guid,
						room.getString("name"),
						room.getString("description"),
						venueId);
				roomMap.put(guid, roomId);
			}
			Log.d("SUSEConferences", "Tracks");
			publishProgress("tracks");
			JSONObject tracksReply = HTTPWrapper.get(tracksUrl);
			JSONArray tracks = tracksReply.getJSONArray("tracks");
			int tracksLen = tracks.length();
			for (int i = 0; i < tracksLen; i++) {
				JSONObject track = tracks.getJSONObject(i);
				String guid = track.getString("guid");
				Long trackId = db.insertTrack(guid,
						track.getString("name"),
						track.getString("color"),
						mConference.getSqlId());
				trackMap.put(guid, trackId);
			}
			Log.d("SUSEConferences", "Speakers");
			publishProgress("speakers");
			JSONObject speakersReply = HTTPWrapper.get(speakersUrl);
			JSONArray speakers = speakersReply.getJSONArray("speakers");
			int speakersLen = speakers.length();
			for (int i = 0; i < speakersLen; i++) {
				JSONObject speaker = speakers.getJSONObject(i);
				String guid = speaker.getString("guid");
				Long speakerId = db.insertSpeaker(guid,
						speaker.getString("name"),
						speaker.getString("company"),
						speaker.getString("biography"),
						"");
				speakerMap.put(guid, speakerId);
			}

			Log.d("SUSEConferences", "Events");
			publishProgress("events");
			JSONObject eventsReply = HTTPWrapper.get(eventsUrl);
			JSONArray events = eventsReply.getJSONArray("events");
			int eventsLen = events.length();
			for (int i = 0; i < eventsLen; i++) {
				JSONObject event = events.getJSONObject(i);
				String guid = event.getString("guid");
				String track = event.getString("track");
				Long trackId = trackMap.get(track);
				Long roomId = roomMap.get(event.getString("room"));
				if (track.equals("meta")) {
					// The "meta" track is used to insert information
					// into the schedule that automatically appears on "my schedule",
					// and also isn't clickable.
					db.insertEvent(guid,
							mConference.getSqlId(),
							roomId.longValue(),
							trackId.longValue(),
							event.getString("date"),
							event.getInt("length"),
							"",
							"",
							event.getString("title"),
							"",
							"");
				} else {
					Long eventId = db.insertEvent(guid,
							mConference.getSqlId(),
							roomId.longValue(),
							trackId.longValue(),
							event.getString("date"),
							event.getInt("length"),
							event.getString("type"),
							event.getString("language"),
							event.getString("title"),
							event.getString("abstract"),
							"");

					JSONArray eventSpeakers = event.getJSONArray("speaker_ids");
					int eventSpeakersLen = eventSpeakers.length();
					for (int j = 0; j < eventSpeakersLen; j++) {
						Long speakerId = speakerMap.get(eventSpeakers.getString(j));
						if (speakerId != null)
							db.insertEventSpeaker(speakerId, eventId);
					}
				}
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			mErrorMessage = e.getLocalizedMessage();
			returnVal = Long.valueOf(-1);
		} catch (SocketException e) {
			e.printStackTrace();
			mErrorMessage = e.getLocalizedMessage();
			returnVal = Long.valueOf(-1);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			mErrorMessage = e.getLocalizedMessage();
			returnVal = Long.valueOf(-1);
		} catch (IOException e) {
			e.printStackTrace();
			mErrorMessage = e.getLocalizedMessage();
			returnVal = Long.valueOf(-1);
		} catch (JSONException e) {
			e.printStackTrace();
			mErrorMessage = e.getLocalizedMessage();
			returnVal = Long.valueOf(-1);
		} 

		if (returnVal == null)
			returnVal = mConference.getSqlId();
		return returnVal;
	}

	protected void onPostExecute(Long id) {
		if (id != -1) {
			db.setConferenceAsCached(id, 1);
		}
		
		mDialog.dismiss();
		this.mListener.conferenceCached(id, mErrorMessage);
	}


}
