/*
 *    Copyright 2013 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.appnexus.opensdk.utils;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public abstract class HTTPGet<Params extends Void, Progress extends Void, Result extends HTTPResponse> extends AsyncTask<Params, Progress, Result> {


    public HTTPGet() {
        super();
    }

    @SuppressWarnings("unchecked")
	@Override
    protected HTTPResponse doInBackground(Void... params) {
        HTTPResponse out = new HTTPResponse();

        HttpClient httpc = new DefaultHttpClient();
        try {
            URI uri = new URI(getUrl());
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse r = httpc.execute(request);

            out.setHeaders(r.getAllHeaders());
            out.setResponseBody(EntityUtils.toString(r.getEntity()));
            boolean isStatusOK = (r.getStatusLine() != null)
                    && (r.getStatusLine().getStatusCode()
                    == 200);
            out.setSucceeded(isStatusOK);
        } catch (URISyntaxException e) {
            out.setSucceeded(false);
        } catch (ClientProtocolException e) {
            out.setSucceeded(false);
        } catch (IOException e) {
            out.setSucceeded(false);
        } finally {
            httpc.getConnectionManager().shutdown();
        }

        return out;
    }

    @Override
    abstract protected void onPostExecute(HTTPResponse response);

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCancelled(HTTPResponse response) {
        super.onCancelled(null);
    }


    protected abstract String getUrl();

}
