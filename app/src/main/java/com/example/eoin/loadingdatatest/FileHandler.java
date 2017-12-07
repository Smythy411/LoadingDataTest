package com.example.eoin.loadingdatatest;

import android.content.Context;
import android.util.Log;

import com.opencsv.CSVReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

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

        for (int i = 1; i < list.size(); i++) {
            Log.d(TAG, "Inserting node " + list.get(i)[0]);
            db.insertNode(Long.parseLong(list.get(i)[0]), list.get(i)[1], list.get(i)[2]);
        }
    }//end openCSVFile

    public void parseXml(){
        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            List<String> temp = new ArrayList<>();

            xpp.setInput( new InputStreamReader(ctx.getAssets().open(osmFile)) );
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(TAG,"Start document");
                } else if(eventType == XmlPullParser.START_TAG) {
                    if(xpp.getName().equals("way")) {
                        //Log.d(TAG, "Way: " + xpp.getAttributeValue(null, "id"));
                        temp.add(xpp.getAttributeValue(null, "id"));
                        int eventType2 = xpp.next();
                        boolean endTag = false;
                        while (!endTag) {
                            if (eventType2 == XmlPullParser.START_TAG) {
                                if(xpp.getName().equals("nd")) {
                                    //Log.d(TAG, "Node: " + xpp.getAttributeValue(null, "ref"));
                                    temp.add(xpp.getAttributeValue(null, "ref"));
                                }
                            } else if (eventType2 == XmlPullParser.END_TAG) {
                                if (xpp.getName().equals("way")) {
                                    Log.d(TAG, "Inserting Way: " + temp.get(0));
                                    for (int i = 1; i < temp.size(); i++) {
                                        //Log.d(TAG, "Node: " + temp.get(i));
                                        db.insertWay(Long.parseLong(temp.get(0)), Long.parseLong(temp.get(i)));
                                    }//end for
                                    temp.clear();
                                    endTag = true;
                                }//End if
                            }//End if else
                            eventType2 = xpp.next();
                        }//End while
                    }//End if
                } else if(eventType == XmlPullParser.END_TAG) {
                    //Log.d(TAG,"End tag "+xpp.getName());
                }//End if else
                eventType = xpp.next();
            }//End while
            Log.d(TAG,"End document");

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }//End try catch
    }//End parseXml()

}//End FileHandler
