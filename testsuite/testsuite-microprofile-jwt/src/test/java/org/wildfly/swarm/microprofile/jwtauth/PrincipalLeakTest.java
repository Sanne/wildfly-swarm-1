/**
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.microprofile.jwtauth;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.microprofile.jwtauth.roles.TestApplication;
import org.wildfly.swarm.microprofile.jwtauth.utils.TokenUtils;
import org.wildfly.swarm.undertow.WARArchive;

import static org.fest.assertions.Assertions.assertThat;
import static org.wildfly.swarm.microprofile.jwtauth.utils.TokenUtils.createToken;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 8/8/18
 */
@RunWith(Arquillian.class)
public class PrincipalLeakTest {
    @Deployment
    public static Archive<?> createDeployment() {
        return initDeployment().addAsResource("project-no-roles-props.yml", "project-defaults.yml");
    }

    protected static WARArchive initDeployment() {
        WARArchive deployment = ShrinkWrap.create(WARArchive.class);
        deployment.addClass(SubjectExposingResource.class);
        deployment.addClass(TestApplication.class);
        deployment.addAsManifestResource(new ClassLoaderAsset("keys/public-key.pem"), "/MP-JWT-SIGNER");
        return deployment;
    }

    @RunAsClient
    @Test
    public void subjectShouldNotLeakToNonSecuredRequest() throws Exception {
        long start = System.currentTimeMillis();
        try {
            // By default, there are 64 threads/tasks
            // tests suggest they are assigned in a round-robin fashion.
            // Therefore 65 requests are used to test if there's no leak.
            for (int i = 0; i < 33; i++) {
                String response = Request.Get("http://localhost:8080/mpjwt/subject/secured")
                        .setHeader("Authorization", "Bearer " + createToken("MappedRole"))
                        .execute().returnContent().asString();
                assertThat(response).isEqualTo(TokenUtils.SUBJECT);
            }
            for (int i = 0; i < 32; i++) {
                Content content = Request.Get("http://localhost:8080/mpjwt/subject/unsecured")
                        .execute().returnContent();
                assertThat(content).isNull();
            }
        } finally {
            System.out.println("subjectShouldNotLeakToNonSecuredRequest time: " + (System.currentTimeMillis() - start));
        }
    }

}