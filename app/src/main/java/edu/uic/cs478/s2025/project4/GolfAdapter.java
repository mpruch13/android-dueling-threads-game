/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 Adapter class for displaying car images in the MainActivity GridView. The View created
 for each grid cell is a vertical LinearLayout containing an ImageView for displaying
 the car thumbnail image and a TextView for displaying the car model name.

 */

package edu.uic.cs478.s2025.project4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

public class GolfAdapter extends BaseAdapter {

    // Fields for context and resource ids.
    private Context mContext;
    private ArrayList<Integer> mHoles;


    /**
     * Constructor
     *
     * @param  c Context from the activity that is creating this adapter
     * @param  holeList An ArrayList containing the res ids of car thumbnail images
     */
    public GolfAdapter(Context c, ArrayList<Integer> holeList){
        mContext = c;
        this.mHoles = holeList;
    }

    @Override
    public int getCount() {
        return mHoles.size();
    }

    @Override
    public Object getItem(int position) {
        return mHoles.get(position);
    }

    @Override
    public long getItemId(int position) {return mHoles.get(position); }

    public ArrayList<Integer> getResourceList(){
        return mHoles;
    }

    public long setImage(int pos, Integer newResId){
        long oldID = getItemId(pos);
        mHoles.set(pos, newResId);
        return oldID;
    }

    /**
     * Recycles or creates a new View for cells that go in the MainActivity
     * GridView.
     *
     * @return Returns a vertical LinearLayout containing a car thumbnail image
     * and a TextView containing the car's model name.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // View is a linear layout containing
        ImageView golfHoleView = (ImageView) convertView;

        // if convertView is not recycled, create it from xml with LayoutInflater
        if (golfHoleView == null) {
            LayoutInflater lf = LayoutInflater.from(mContext);
            golfHoleView = (ImageView) lf.inflate(R.layout.hole, parent, false);
        }

        // Put data (actual image reference) in imageView + return it
        golfHoleView.setImageResource(mHoles.get(position));
        return golfHoleView;
    }
}
