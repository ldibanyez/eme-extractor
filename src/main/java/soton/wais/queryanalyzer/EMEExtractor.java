/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package soton.wais.queryanalyzer;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author ldig
 */
public class EMEExtractor {

	Query query;


	public EMEExtractor(String querystr){
	

		query  = QueryFactory.create(querystr);
		//query  = QueryFactory.create(querystr,Syntax.syntaxSPARQL_10);
	}

	
	public TriplePath getMeasureBGP(Var measure) throws Exception{
		// Assume that the instantiation of the measure is a literal, therefore
		// only one triple pattern has it, and it has it as object.
		final TriplePath[] measurebgp = new TriplePath[1];
	
	ElementWalker.walk(query.getQueryPattern(),
			  // For each element...
			  new ElementVisitorBase() {
				  // ...when it's a block of triples...
				  @Override
				  public void visit(ElementPathBlock el) {
					  // ...go through all the triples...
					  Iterator<TriplePath> patterns = el.patternElts();
					  while (patterns.hasNext()) {
						  // ...and find the predicate of the one with 
						  TriplePath pattern = patterns.next();
						  if(pattern.getObject().isVariable()){
						  	Var fromQuery = Var.alloc(pattern.getObject());
						 	 if(fromQuery.equals(measure)){
								// 
								measurebgp[0] = pattern;
							 }
						  }
					  }
				}
			}
		);

	if(measurebgp.length < 1){
	   throw new Exception("Variable not found in the query");	
	}else{
	   return measurebgp[0];	
	}
	}

public Set<TriplePath> patternsWithVar(Var var){
	
	final Set<TriplePath> pats = new HashSet<>();
		ElementWalker.walk(query.getQueryPattern(),
			  // For each element...
			  new ElementVisitorBase() {
				  // ...when it's a block of triples...
				  @Override
				  public void visit(ElementPathBlock el) {
					  // ...go through all the triples...
					  Iterator<TriplePath> patterns = el.patternElts();
					  while (patterns.hasNext()) {
						  // ...and find the predicate of the one with 
						  TriplePath pattern = patterns.next();
						  if(pattern.getObject().equals(var) || 
								  pattern.getSubject().equals(var) ||
								  pattern.getPredicate().equals(var)){
							 
						  	pats.add(pattern);
						  }
							
						  }
					  }
			    }
		);
return pats;

}
	
	public Set<Node> getTypes(Var var){
	
	Set<Node> types = new HashSet<>();
	Set<TriplePath> pats = patternsWithVar(var);
	//System.out.println(var);

	 // If rdf:type is explicit, this is our dimension type
	 for (TriplePath tp : pats){
		 
	   // System.out.println(tp.getPredicate());
	    if(tp.getPredicate().equals(RDF.Nodes.type)){
		   if(tp.getObject().isURI()){
			 types.add(tp.getObject());
		     return types;
			}
		} 
	 }

	 // If no explicit type, we need to dereference the predicates 

	 Model m = ModelFactory.createDefaultModel();
	 
	 for (TriplePath tp : pats){
		 try{
		 RDFDataMgr.read(m,tp.getPredicate().getURI());
		if(tp.getSubject().equals(var)){
			Statement domain = m.getProperty(m.getResource(tp.getPredicate().getURI()), RDFS.domain);
			RDFNode object = domain.getObject();
			types.add(NodeFactory.createURI(object.asResource().getURI()));
		
		}

		 
	    if(tp.getObject().equals(var)){
			Statement domain = m.getProperty(m.getResource(tp.getPredicate().getURI()), RDFS.range);
			RDFNode object = domain.getObject();
			types.add(NodeFactory.createURI(object.asResource().getURI()));
		} 

		 }
		 catch(HttpException notfound){
			 System.out.println("Undereferenceable predicate");
		 
		 }
	 }
	 
	
	return types;
	
	}
	public Set<SummarizabilityStatement> extract() throws Exception{

	Set<SummarizabilityStatement> extraction = new HashSet<>();
		

	
	 List<ExprAggregator> aggregationExprs = query.getAggregators();
		//System.out.println(aggregationExprs);
	String aggregationFunction = "";
	Set<Node> dimensionset = new HashSet<>();
		for (ExprAggregator exp : aggregationExprs){
			aggregationFunction = exp.getAggregator().getName();
			// Assumption, only one var mentioned
		    Set<Var> AggVar = exp.getAggregator().getExprList().getVarsMentioned();
			if(AggVar.size() > 1){
			  throw new Exception("Query with more than one variable in an aggregation not supported");
			}
			for (Var v : AggVar){
		    TriplePath measureBGP = getMeasureBGP(v);
			Node measure = measureBGP.getPredicate(); 

			if(query.hasGroupBy()){
				//System.out.println(query.getGroupBy().getVars().size());
			   for (Var gbv : query.getGroupBy().getVars()){
			   //   System.out.println(gbv);
				 Set<TriplePath> pats = patternsWithVar(gbv);
			//	 System.out.println(pats);
			  	 Set<Node> dimensions = getTypes(gbv); 
			  	 dimensionset.addAll(dimensions);
			   }

			}
			else{
				Node measureSubject = measureBGP.getSubject();
		//		System.out.println(measureSubject);
				if(measureSubject.isVariable()){
			    	Var msub = Var.alloc(measureSubject);
					 Set<TriplePath> pats = patternsWithVar(msub);
					 Set<Node> dimensions = getTypes(msub); 
					 dimensionset.addAll(dimensions);
				}			
			}

			if(!dimensionset.isEmpty()){
				extraction.add(new SummarizabilityStatement(measure,aggregationFunction,dimensionset));
			}else{
				throw new Exception("Could not extract summarizability statement from this query");
		//	System.out.println(query.toString());
			}
		    
			}
		    
		
		} 
		return extraction;
	}
	
	public static void main(String[] args) throws Exception {

		
	String querystr ="PREFIX dbo:<http://dbpedia.org/ontology/>"
			 + "SELECT ?movie (MAX(?runtime) as ?maxruntime) (avg(?runtime) as ?avgrt) WHERE {"
			 + "?movie dbo:runtime ?runtime ."
			 + "?movie dbo:starring <http://dbpedia.org/resource/Jason_Statham>." +
			 "<http://dbpedia.org/resource/Jason_Statham> dbo:starring ?star ."
		 	//+ "_:b1 <http://example.org/blankness> _:b2 ."
		 	+ "?movie <http://example.org/literally> '42' ." 
			 + "} GROUP BY ?movie"; 	
	
	String querystr2 ="PREFIX dbo:<http://dbpedia.org/ontology/>"
			 + "SELECT ?movie (MAX(?runtime) as ?maxruntime) (avg(?runtime) as ?avgrt) WHERE {"
			 + "?movie dbo:runtime ?runtime ."
			 + "?movie dbo:starring <http://dbpedia.org/resource/Jason_Statham>." 
			 + "?movie dbo:producer ?producer ."
			 + "} GROUP BY ?movie ?producer"; 

	String querystr3 ="PREFIX mecontrib: <http://rdf.myexperiment.org/ontologies/contributions/>" +
					"PREFIX mevd: <http://rdf.myexperiment.org/ontologies/base/>" +
				"PREFIX sioc: <http://rdfs.org/sioc/ns#> " +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
				"SELECT ?user (SUM(?downloaded) AS ?total_downloads)" +
				"WHERE{" +
				"  ?workflow rdf:type mecontrib:Workflow ;" +
				"    sioc:has_owner ?user ;" +
				"    mevd:downloaded ?downloaded" +
				"}" +
				"GROUP BY ?user";

	String querystr4 ="PREFIX dbo:<http://dbpedia.org/ontology/>"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			 + "SELECT ?movie (MAX(?runtime) as ?maxruntime) (avg(?runtime) as ?avgrt) WHERE {"
			 + "?movie dbo:runtime ?runtime ."
			 + "?movie dbo:starring <http://dbpedia.org/resource/Jason_Statham>." +
			 "<http://dbpedia.org/resource/Jason_Statham> dbo:starring ?star ."
		 	//+ "_:b1 <http://example.org/blankness> _:b2 ."
		 	+ "?movie rdf:type dbo:movie ." 
			 + "} GROUP BY ?movie"; 

String querystr5 = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"PREFIX  type: <http://dbpedia.org/class/yago/>\n" +
"PREFIX  prop: <http://dbpedia.org/property/>\n" +
"\n" +
"SELECT DISTINCT  (MAX(strlen(?country_name)) AS ?countryNameLength)\n" +
"WHERE\n" +
"  { ?country  a                     type:LandlockedCountries ;\n" +
"              rdfs:label            ?country_name ;\n" +
"              prop:populationEstimate  ?population\n" +
"    FILTER langMatches(lang(?country_name), \"en\")\n" +
"  }\n" +
"LIMIT   1";
	
/*

	Set<String> queries = new HashSet<>();
	queries.add(querystr5);
	//queries.add(querystr3);
	//queries.add(querystr2);
	//queries.add(querystr);
	
	for(String q : queries){
		EMEExtractor mda = new EMEExtractor(q);
	
	   Set<SummarizabilityStatement> extraction = mda.extract();	
		
	}
*/	

	
	
	
	String 	infilepath = "Queries per CLF log";
	String 	outputpath = "RDF output";
	Model sumKB = ModelFactory.createDefaultModel();
	
	
	try (Scanner scanner = new Scanner(new File(infilepath))) {
	
	    while(scanner.hasNext()){
			String querystring = scanner.nextLine();
			try{
			EMEExtractor mda = new EMEExtractor(URLDecoder.decode(querystring));
			Set<SummarizabilityStatement> extraction = mda.extract();
			for (SummarizabilityStatement ss : extraction){
			  sumKB.add(ss.toRDF());
			}
			} 
			catch(NullPointerException npe){
			 
			   System.out.println(URLDecoder.decode(querystring));
			  npe.printStackTrace();
			}
			catch(UnsupportedOperationException uoe){
			 
			   System.out.println(URLDecoder.decode(querystring));
			  uoe.printStackTrace();
			}
			catch (Exception e){
			   System.out.println(URLDecoder.decode(querystring));
			   e.printStackTrace();
			   
			}
			
		}
	
		}
		
	
	sumKB.write(Files.newBufferedWriter(Paths.get(outputpath),StandardOpenOption.TRUNCATE_EXISTING),"TURTLE");
	

	}
}
