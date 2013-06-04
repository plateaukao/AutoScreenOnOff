package com.danielkao.autoscreenonoff.tests;

import android.test.ServiceTestCase;
import com.danielkao.autoscreenonoff.SensorMonitorService;

/**
 * Created by plateau on 2013/06/04.
 */
public class SensorServiceTest extends ServiceTestCase<SensorMonitorService>{
    SensorMonitorService ss;
    public SensorServiceTest(Class<SensorMonitorService> serviceClass) {
        super(serviceClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ss = (SensorMonitorService)getService();
    }

    public void testAutoOn(){
        assertTrue(true);
    }
}
