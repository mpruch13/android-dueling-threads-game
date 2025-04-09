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
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.List;

public class holeAdapter extends BaseAdapter {

    // Fields for context and resource ids.
    private Context mContext;
    private List<Boolean> holes;


    /**
     * Constructor
     *
     * @param  c Context from the activity that is creating this adapter
     * @param  imgIds An ArrayList containing the res ids of car thumbnail images
     * @param  txtIds An ArrayList containing the res ids of car model name strings
     */
    public holeAdapter(Context c, List<Boolean> holeList){
        mContext = c;
        this.holes = holeList;
    }

    @Override
    public int getCount() {
        return holes.size();
    }

    @Override
    public Object getItem(int position) {
        return holes.get(position);
    }

    @Override
    public long getItemId(int position) {
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
        LinearLayoutCompat linLayout = (LinearLayoutCompat) convertView;
        Checkbox

        // if convertView is not recycled, create it from xml with LayoutInflater
        if (linLayout == null) {
            LayoutInflater lf = LayoutInflater.from(mContext);
            linLayout = (LinearLayoutCompat)lf.inflate(R.layout.grid_cell, parent, false);
        }

        // Set the correct image and text data based on the grid position, and return the View
        imageview = linLayout.findViewById(R.id.grid_image_view);
        textview = linLayout.findViewById(R.id.grid_text_view);
        imageview.setImageResource(mThumbIds.get(position));
        textview.setText(mTextIds.get(position));
        return linLayout;
    }
}
