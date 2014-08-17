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
public class RoundTripTest extends BaseTest {

    @Test
    public void fromDlcToEcs() {
        final String dataSetLabel = "test" + (Math.round(Math.random() * 10000));
        //Create DataSet
        RestAssured.given()
                .auth().basic("admin", "admin").formParam("pipe_label", dataSetLabel)
                .redirects().follow(false).expect().statusCode(HttpStatus.SC_SEE_OTHER).when()
                .post("/sourcing/create_pipe");
        //ISSUE: the above doesn't return the name of the created dataset
        final String dataSetName = "urn:x-localinstance:/dlc/" + dataSetLabel;

        //Start batch processing
        final Response processBatchResponse = RestAssured.given()
                .redirects().follow(false)
                .auth().basic("admin", "admin")
                .formParam("dataSet", dataSetName)
                .formParam("rdfizer", "patent")
                .formParam("digester", "patent")
                .formParam("interlinker", "silk-patents")
                .formParam("skipPreviouslyAdded", "on")
                .formParam("smushAndPublish", "on")
                .formParam("recurse", "on")
                .formParam("maxFiles", "2")
                .formParam("url", "http://raw.fusepool.info/IREC/EP/")
                .expect().statusCode(HttpStatus.SC_SEE_OTHER).when()
                .post("/sourcing/processBatch/");
        //ISSUE: the task is started without checking if the dataset and interlinker exists

        final String taskLocation = processBatchResponse.getHeader("Location");

        //Making sure the tasks ends after a while
        int i = 0;
        while (true) {
            if (i++ == 120) {
                throw new RuntimeException("Did not end after two minutes: " + taskLocation);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            final Response taskResponse = RestAssured.given()
                    .header("Accept", "text/html")
                    .expect()
                    .body(containsString("started"))
                    .get(taskLocation);
            if (taskResponse.getBody().asString().contains("ended")) {
                break;
            }
        }
        //it may take a while till the data is reindex and available in ECS
        Graph graph;
        i = 0;
        while (true) {
            //now ecs should find some data
            final Response ecsResponse = RestAssured.given()
                    .header("Accept", "text/turtle")
                    .queryParam("search", "*")
                    .expect().statusCode(HttpStatus.SC_OK)
                    .header("Content-Type", "text/turtle").when()
                    .get("/ecs/");
            graph = Parser.getInstance().parse(
                    new ByteArrayInputStream(ecsResponse.asByteArray()),
                    SupportedFormat.TURTLE);
            if (graph.size() > 10) {
                break;
            }
            if (i++ == 158) {
                throw new RuntimeException("Did not found triples in ECS result even after 8 minute");
            }
            try {
                if (i >= 100) {
                    if (i >= 150) {
                        Thread.sleep(30000);
                    } else {
                        Thread.sleep(4000);
                    }
                } else {
                    Thread.sleep(400);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        //Assert.assertTrue("The graph returned by ecs seems too small (only " + graph.size() + " triples)", graph.size() > 10);
        GraphNode storeViewType = new GraphNode(ECS.ContentStoreView, graph);
        GraphNode storeView = storeViewType.getSubjectNodes(RDF.type).next();
        Literal contentsCount = storeView.getLiterals(ECS.contentsCount).next();
        Assert.assertTrue("No content found.", Integer.parseInt(contentsCount.getLexicalForm()) > 0);
    }

}
