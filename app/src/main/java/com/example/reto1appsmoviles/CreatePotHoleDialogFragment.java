package com.example.reto1appsmoviles;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.example.reto1appsmoviles.comm.HTTPSWebUtilDomi;
import com.example.reto1appsmoviles.models.PotHole;
import com.google.gson.Gson;

import java.util.UUID;

public class CreatePotHoleDialogFragment extends DialogFragment {

    private PotHole potHole;
    Activity  activity;
    private HTTPSWebUtilDomi https;
    private Gson gson;

    public CreatePotHoleDialogFragment(PotHole potHole, Activity activity){
        this.potHole =potHole;
        this.activity = activity;
        https = new HTTPSWebUtilDomi();
        gson = new Gson();
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Coordenada:\n"+this.potHole.latitude+", "+this.potHole.longitude+"\n\nDirección:\n"+this.potHole.streetAddress).setTitle("Agregar un  hueco")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new Thread(
                                ()->{
                                    String url = "https://appmoviles-47f25.firebaseio.com/potholes/"+ UUID.randomUUID().toString() +".json";
                                    https.PUTrequest(url,gson.toJson(potHole));
                                }
                        ).start();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
