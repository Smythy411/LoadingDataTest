package com.example.eoin.loadingdatatest;

import android.content.Context;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eoin on 04/12/2017.
 */

public class FileHandler {
    private String csvFile;
    private String osmFile;

    private Context ctx;
    private DBManager db;
    private String next[] = {};
    private List<String[]> list = new ArrayList<>();

    public FileHandler (String cFile, String oFile, Context context) {
        this.csvFile = cFile;
        this.osmFile = oFile;

        this.ctx = context;
        db = new DBManager(ctx);
        db.open();
    }//End FileHandler constructor

    public void openCSVFile() {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(ctx.getAssets().open(csvFile)), '\t');//Specify asset file name
            //in open();
            for (; ; ) {
                next = reader.readNext();
                if (next != null) {
                    list.add(next);
                } else {
                    break;
                }//end if else
            }//end for
        } catch (IOException e) {
            e.printStackTrace();
        }//end try catch

        for (int i = 1; i < 50; i++) {
            db.insertNode(Integer.parseInt(list.get(i)[0]), list.get(i)[1], list.get(i)[2]);
        }
    }//end openCSVFile

}//End FileHandler
