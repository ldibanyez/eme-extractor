/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package soton.wais.queryanalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.expr.ExprAggregator;

/**
 *
 * @author ldig
 */
public class LogParser {
	

	public static void filterQueries(String infilepath, String outfilepath){
		
		int queryparsefails = 0;
		int queryexceptions = 0;
		
		Path path = Paths.get(outfilepath);
List<String> querylist = new ArrayList<>();
		try (Scanner scanner = new Scanner(new File(infilepath))) {
			String regexString = Pattern.quote("query=") + "(.*?)" + Pattern.quote(" HTTP");
			Pattern pattern = Pattern.compile(regexString);
			while(scanner.hasNext()){
				String line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
			String querystring = "";
			while (matcher.find()) {
			  querystring = matcher.group(1); // Since (.*?) is capturing group 1
			  // You can insert match into a List/Collection here
			  String[] paramcheck = querystring.split("&");
			  if (paramcheck.length > 1){
				  querystring = paramcheck[0];
			  }
			}
			//System.out.println(querystring);
			int prefixindex = querystring.indexOf("PREFIX");
			if(prefixindex > 0){
				querystring = querystring.substring(prefixindex);
			}
			//System.out.println(URLDecoder.decode(querystring));
			
			try{				
				Query q = QueryFactory.create(URLDecoder.decode(querystring));				
				if(q.hasAggregators()){
					List<ExprAggregator> aggregators = q.getAggregators();
					if(aggregators.size() == 1 && 
						(aggregators.get(0).getAggregator().getName().equals("COUNT")
						|| aggregators.get(0).getAggregator().getName().equals("SAMPLE"))){
					// This contains only a COUNT, Ignore						
	                		} else {
						querylist.add(querystring);
					}

								
				}
			}catch (QueryParseException qpe){
			    queryparsefails++;
			}catch (Exception qex){
				queryexceptions++;
			}
			
			
	
			
			}
		
		}catch (IOException e) {
			e.printStackTrace();
		} 
		
		try (BufferedWriter writer = Files.newBufferedWriter(path,StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {					for(String s : querylist){
					writer.write(s);
					writer.newLine();
		}
					} catch (IOException ex) {	
			Logger.getLogger(LogParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		System.out.println("Query parse exceptions "+queryparsefails);
		System.out.println("Other query exceptions "+queryexceptions);
	
	}
	

	public static void main(String[] args) throws IOException, Exception {
	String 	fileName = "PATH"
		+ "";


	filterQueries(fileName,fileName+".agg");



/*
int parseExceptions = 0;
    try (Scanner scanner = new Scanner(new File(fileName))) {

	Set<SummarizabilityStatement> allextraction = new HashSet<>();
			while (scanner.hasNext()){
				try {
				MDAExtractor mda = new MDAExtractor(scanner.nextLine());
		Set<SummarizabilityStatement> extraction = mda.extract();
		allextraction.addAll(extraction);
				}catch(QueryParseException qpe){
			parseExceptions++;
		}

			}
	System.out.println(parseExceptions);
	System.out.println(allextraction);
		} catch (IOException e) {
			e.printStackTrace();
		}     
    //Close the stream and it's underlying file as well
	}
*/
	}
}
