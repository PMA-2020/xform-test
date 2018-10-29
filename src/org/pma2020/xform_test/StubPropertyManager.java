/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pma2020.xform_test;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

import java.util.HashSet;
import java.util.List;

/**
 * Stub for validation purposes.
 * 
 * @author mitchellsundt@gmail.com
 */

public class StubPropertyManager implements IPropertyManager {


    private final static String DEVICE_ID_PROPERTY = "deviceid"; // imei
    private final static String SUBSCRIBER_ID_PROPERTY = "subscriberid"; // imsi
    private final static String SIM_SERIAL_PROPERTY = "simserial";
    private final static String PHONE_NUMBER_PROPERTY = "phonenumber";
    private final static String USERNAME = "username";
    private final static String EMAIL = "email";

    private final static String OR_DEVICE_ID_PROPERTY = "uri:deviceid"; // imei
    private final static String OR_SUBSCRIBER_ID_PROPERTY = "uri:subscriberid"; // imsi
    private final static String OR_SIM_SERIAL_PROPERTY = "uri:simserial";
    private final static String OR_PHONE_NUMBER_PROPERTY = "uri:phonenumber";
    private final static String OR_USERNAME = "uri:username";
    private final static String OR_EMAIL = "uri:email";

    private static HashSet<String> mProperties;
    
    static {
        mProperties = new HashSet<>();
        mProperties.add(DEVICE_ID_PROPERTY);
        mProperties.add(SUBSCRIBER_ID_PROPERTY);
        mProperties.add(SIM_SERIAL_PROPERTY);
        mProperties.add(PHONE_NUMBER_PROPERTY);
        mProperties.add(USERNAME);
        mProperties.add(EMAIL);
        mProperties.add(OR_DEVICE_ID_PROPERTY);
        mProperties.add(OR_SUBSCRIBER_ID_PROPERTY);
        mProperties.add(OR_SIM_SERIAL_PROPERTY);
        mProperties.add(OR_PHONE_NUMBER_PROPERTY);
        mProperties.add(OR_USERNAME);
        mProperties.add(OR_EMAIL);
    }

    StubPropertyManager() {}

    public void addRules(IPropertyRules rules) {}


    public List<IPropertyRules> getRules() {
        return null;
    }

    public List<String> getProperty(String arg0) {
        return null;
    }

    public String getSingularProperty(String propertyName) {
        if ( mProperties.contains(propertyName)) {
            return "found";
        }
        if ( propertyName != null && propertyName.length() != 0 ) {
            throw new IllegalArgumentException("Unrecognized property name: " + propertyName);
        }
        return "notfound";
    }


    public void setProperty(String propertyName, String propertyValue) {
    }


    public void setProperty(String propertyName, List<String> propertyValue) {
    }

}
