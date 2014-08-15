/*
 * Copyright 2014 reto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fusepool.tests;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import eu.fusepool.ecs.ontologies.ECS;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.http.HttpStatus;
import static org.hamcrest.Matchers.*;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author reto
 */
public class DlcTest extends BaseTest {
    
    /* Currently the form is returned without pwd
    @Test
    public void requireAuth() {
        Response response = RestAssured.given()
                .expect().statusCode(HttpStatus.SC_UNAUTHORIZED).when()
                .get("/sourcing");
    }
    */
    
    @Test
    public void htmlVersion() {
        Response response = RestAssured.given().header("Accept", "text/html")
                .auth().basic("admin", "admin")
                .expect().statusCode(HttpStatus.SC_OK).when()
                .get("/sourcing");
        response.then().assertThat().body(containsString("Fusepool"));
    }
    
   
    
}
