package edu.uic.cs478.s2025.project4;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class EndGameDialogFragment extends DialogFragment {

    public interface EndGameDialogListener{
        void onStartNewGame();
        void onQuit();
    }

    private EndGameDialogListener listener;
    protected static final String ARG_DIALOG_MSG = "msg";


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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction.
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
        // Create the AlertDialog object and return it.
        return builder.create();
    }
}