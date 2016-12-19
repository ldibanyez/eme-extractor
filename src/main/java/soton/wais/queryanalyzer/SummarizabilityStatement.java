/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package soton.wais.queryanalyzer;

import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;

/**
 *
 * @author ldig
 */
public class SummarizabilityStatement {

	Node measure;
	String aggregationFunction;
	Set<Node> dimensions;

	public SummarizabilityStatement(Node measure, String aggregationFunction, Set<Node> dimensions) {
		this.measure = measure;
		this.aggregationFunction = aggregationFunction;
		this.dimensions = dimensions;
	}

	@Override
	public String toString(){
	return "Measure "+ measure.toString() + " has been considered summarizable by "+
		aggregationFunction +" across dimensions " + dimensions.toString();
	}

	public Model toRDF(){

	Model result = ModelFactory.createDefaultModel();
	String m = "<" + measure.getURI() + ">";
	String f = "";
	switch(aggregationFunction){
	
		case "AVG": f= "qb4o:Avg";
			break;
		case "MAX": f = "qb4o:Max";
			break;
		case "COUNT": f = "qb4o:Count";
			break;
		case "SAMPLE:": f = "qb4o:Sample";
			break;
		case "MIN": f = "qb4o:Min";
			break;
		case "SUM": f = "qb4o:Sum";
			break;
		default: f = "qb4o:"+aggregationFunction;
			break;
	};

	String insertquery= ""
		+ "PREFIX qb4o: <http://purl.org/qb4olap/cubes#> "
		+ "PREFIX qb: <http://purl.org/linked-data/cube#> "
		+ "INSERT DATA{"
		+ m + " qb4o:has_summarizability_statement _:b0 . "
		+ "_:b0 qb4o:by_aggregation "+ f +". ";
	for(Node dim : dimensions){
	insertquery +=
	        "_:b0 qb4o:across_dimension <" + dim.getURI() + ">. ";
	}
	insertquery += "}";
	try{
	UpdateAction.parseExecute(insertquery, result);
	} catch(QueryParseException qpe){
	    System.out.println(insertquery);
	    qpe.printStackTrace();
	}

	return result;	
	}


	
	
}
