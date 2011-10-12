/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hyracks.control.cc.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.uci.ics.hyracks.control.cc.ClusterControllerService;
import edu.uci.ics.hyracks.control.common.application.ApplicationContext;
import edu.uci.ics.hyracks.control.common.work.SynchronizableWork;

public class ApplicationInstallationHandler extends AbstractHandler {
    private ClusterControllerService ccs;

    public ApplicationInstallationHandler(ClusterControllerService ccs) {
        this.ccs = ccs;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            while (target.startsWith("/")) {
                target = target.substring(1);
            }
            while (target.endsWith("/")) {
                target = target.substring(0, target.length() - 1);
            }
            String[] parts = target.split("/");
            if (parts.length != 1) {
                return;
            }
            final String appName = parts[0];
            if (HttpMethods.PUT.equals(request.getMethod())) {
                class OutputStreamGetter extends SynchronizableWork {
                    private OutputStream os;

                    @Override
                    protected void doRun() throws Exception {
                        ApplicationContext appCtx;
                        appCtx = ccs.getApplicationMap().get(appName);
                        if (appCtx != null) {
                            os = appCtx.getHarOutputStream();
                        }
                    }
                }
                OutputStreamGetter r = new OutputStreamGetter();
                try {
                    ccs.getJobQueue().scheduleAndSync(r);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                try {
                    IOUtils.copyLarge(request.getInputStream(), r.os);
                } finally {
                    r.os.close();
                }
            } else if (HttpMethods.GET.equals(request.getMethod())) {
                class InputStreamGetter extends SynchronizableWork {
                    private InputStream is;

                    @Override
                    protected void doRun() throws Exception {
                        ApplicationContext appCtx;
                        appCtx = ccs.getApplicationMap().get(appName);
                        if (appCtx != null && appCtx.containsHar()) {
                            is = appCtx.getHarInputStream();
                        }
                    }
                }
                InputStreamGetter r = new InputStreamGetter();
                try {
                    ccs.getJobQueue().scheduleAndSync(r);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                if (r.is == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    response.setContentType("application/octet-stream");
                    response.setStatus(HttpServletResponse.SC_OK);
                    try {
                        IOUtils.copyLarge(r.is, response.getOutputStream());
                    } finally {
                        r.is.close();
                    }
                }
            }
            baseRequest.setHandled(true);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}