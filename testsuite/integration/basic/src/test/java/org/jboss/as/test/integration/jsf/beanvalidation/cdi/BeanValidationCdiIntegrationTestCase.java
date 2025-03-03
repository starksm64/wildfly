/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jsf.beanvalidation.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the integration of Jakarta Server Faces, CDI, and Jakarta Bean Validation.
 * TODO. Faces 4 does not support Faces ManagedBeans so this has been adapted
 * to only use CDI beans. The effect is this is no longer really a test of the Faces
 * integration with Bean Validation. It does however test CDI + BV. Presumably that's
 * well covered elsewhere though.
 *
 *
 * @author Farah Juma
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BeanValidationCdiIntegrationTestCase {
    @ArquillianResource
    private URL url;

    private final Pattern viewStatePattern = Pattern.compile("id=\".*javax.faces.ViewState.*\" value=\"([^\"]*)\"");
    private final Pattern nameErrorPattern = Pattern.compile("<div id=\"nameError\">([^<]+)</div>");
    private final Pattern numberErrorPattern = Pattern.compile("<div id=\"numberError\">([^<]+)</div>");

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "registration-jsf.war");
        war.addPackage(BeanValidationCdiIntegrationTestCase.class.getPackage());
        war.addAsWebResource(BeanValidationCdiIntegrationTestCase.class.getPackage(), "register.xhtml", "register.xhtml");
        war.addAsWebResource(BeanValidationCdiIntegrationTestCase.class.getPackage(), "confirmation.xhtml", "confirmation.xhtml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsWebInfResource(BeanValidationCdiIntegrationTestCase.class.getPackage(), "faces-config.xml","faces-config.xml");
        return war;
    }

    @Test
    public void testSuccessfulBeanValidation() throws Exception {
        String responseString = registerTeam("Team1", 6);

        Matcher errorMatcher = nameErrorPattern.matcher(responseString);
        assertTrue(responseString, !errorMatcher.find());

        errorMatcher = numberErrorPattern.matcher(responseString);
        assertTrue(responseString, !errorMatcher.find());
    }

    @Test
    public void testFailingBeanValidation() throws Exception {
        String nameError = null;
        String numberError = null;
        String responseString = registerTeam("", 1);

        Matcher errorMatcher = nameErrorPattern.matcher(responseString);
        if (errorMatcher.find()) {
            nameError = errorMatcher.group(1).trim();
        }

        errorMatcher = numberErrorPattern.matcher(responseString);
        if (errorMatcher.find()) {
            numberError = errorMatcher.group(1).trim();
        }

        assertEquals(responseString,"Team name must be at least 3 characters.", nameError);
        assertEquals(responseString, "Not enough people for a team.", numberError);
    }

    private String registerTeam(String name, int numberOfPeople) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();

        try {
            // Create and execute a GET request
            String jsfViewState = null;
            String requestUrl = url.toString() + "register.jsf";
            HttpGet getRequest = new HttpGet(requestUrl);
            HttpResponse response = client.execute(getRequest);
            try {
                String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                assertEquals(responseString, 200, response.getStatusLine().getStatusCode());

                // Get the Jakarta Server Faces view state
                Matcher jsfViewMatcher = viewStatePattern.matcher(responseString);
                if (jsfViewMatcher.find()) {
                    jsfViewState = jsfViewMatcher.group(1);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Create and execute a POST request with the given team name and
            // the given number of people
            HttpPost post = new HttpPost(requestUrl);

            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("javax.faces.ViewState", jsfViewState));
            list.add(new BasicNameValuePair("register", "register"));
            list.add(new BasicNameValuePair("register:inputName", name));
            list.add(new BasicNameValuePair("register:inputNumber", Integer.toString(numberOfPeople)));
            list.add(new BasicNameValuePair("register:registerButton", "Register"));

            post.setEntity(new StringEntity(URLEncodedUtils.format(list, StandardCharsets.UTF_8), ContentType.APPLICATION_FORM_URLENCODED));
            response = client.execute(post);

            try {
                String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                assertEquals(responseString, 200, response.getStatusLine().getStatusCode());
                return responseString;
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }
}
