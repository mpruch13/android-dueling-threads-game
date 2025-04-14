/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 Adapter class for displaying golf hole images in the GameActivity GridView. Very simple, just
 uses a single ImageView for each hole in the golf hole array. Gets dynamically updated as the
 game goes along.

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
     * @param  holeList An ArrayList containing the res ids of golf hole images.
     */
    public GolfAdapter(Context c, ArrayList<Integer> holeList){
        mContext = c;
        this.mHoles = holeList;
    }

    /// The
    @Override
    public int getCount() {return mHoles.size();}
    @Override
    public Object getItem(int position) {
        return mHoles.get(position);
    }
    @Override
    public long getItemId(int position) {return mHoles.get(position); }

    /**
     * Returns the resource list of golf holes used in the adapter.
     * Used for restoring the display after a config change.
     */
    public ArrayList<Integer> getResourceList(){
        return mHoles;
    }

    /**
     * Sets the image at the given position to the image represented by the given
     * resource id. Used to update the display as the game is played.
     */
    public void setImage(int pos, Integer newResId){
        mHoles.set(pos, newResId);
    }

    /**
     * Recycles or creates a new View for cells that go in the GameActivity
     * GridView.
     *
     * @return Returns an ImageView containing a golf hole image.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the golf hole imageview
        ImageView golfHoleView = (ImageView) convertView;

        // if view is not recycled, create it.
        if (golfHoleView == null) {
            LayoutInflater lf = LayoutInflater.from(mContext);
            golfHoleView = (ImageView) lf.inflate(R.layout.hole, parent, false);
        }

        // Put data (actual image reference) in imageView + return it
        golfHoleView.setImageResource(mHoles.get(position));
        return golfHoleView;
    }
}
