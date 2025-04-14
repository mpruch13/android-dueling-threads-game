/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 MainActivity for the Microgolf game. Displays a main menu with two buttons that allow players to either start a game or
 exit the application.

 Class for a dialog Fragment that appears when a game ends. Displays a message that lets the user know the outcome of the game,
 and gives them the option to either start a new game or return to the main menu.
 */

package edu.uic.cs478.s2025.project4;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class EndGameDialogFragment extends DialogFragment {

    /// Listener interface that allows GameActivity to set the methods that
    /// will be run when the user makes a selection.
    public interface EndGameDialogListener{
        void onStartNewGame();
        void onQuit();
    }

    private EndGameDialogListener listener;
    protected static final String ARG_DIALOG_MSG = "msg";


    /** Creates a new instance of the EndGameDialogFragment with a message
     *  from GameActivity (let's the user know the game's outcome)
    */
    public static EndGameDialogFragment newInstance(int messageResId) {
        EndGameDialogFragment fragment = new EndGameDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_MSG, messageResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof EndGameDialogListener) {
            listener = (EndGameDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement EndGameDialogListener");
        }
    }


    /**
     * Creates the dialog with Builder class, sets the text,
     * and creates listeners for the buttons.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int messageResId = R.string.default_dialog_message;
        Bundle args = getArguments();
        if (args != null) {
            messageResId = args.getInt(ARG_DIALOG_MSG, messageResId);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(messageResId)
                .setPositiveButton(R.string.dialog_newgame_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null){
                            listener.onStartNewGame();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_exit_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onQuit();
                    }
                });
        return builder.create();
    }
}