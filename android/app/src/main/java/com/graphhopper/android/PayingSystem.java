package com.graphhopper.android;

/**
 * Created by martinwurflein on 31.03.17.
 */

public interface PayingSystem {

    public float calculatePayingTime(Long timeStart,Long timeEnd);

    public String printPayingInformation();

    /**
     *
     * @return Bill as a String
     */
    public String makeTransaction(long timeStart,long timeEnd,PayingDevice device);

}
