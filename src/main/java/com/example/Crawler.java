package com.example ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Crawler {
	
	private ArrayList<String> url; //URL to be fetched 
	private StringBuffer result ; //storing results from URL
	private Map<String,Integer> wordCount; // TreeMap for storing frequencies of words
	 
	public Crawler(){
		url=new ArrayList<String>();
		result = new StringBuffer();
		wordCount=new TreeMap<String,Integer>();
	}
			
	private void fetchData(String url){ // Fetching the HTML data from the URL specified 
		HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD).build()).build();  //setting configuration for executing request
		HttpGet request = new HttpGet(url);
		HttpResponse response = null;

		try {
			response = client.execute(request); //execute HTTP request
		} catch (ClientProtocolException e) {
				e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int code=response.getStatusLine().getStatusCode(); 
		if(code!=200) // checking for abnormal return code
			System.out.println("Response Code : " + code );

		BufferedReader bufferReader = null;
		try {
			bufferReader = new BufferedReader(
			new InputStreamReader(response.getEntity().getContent())); //reading response from execute HTTP request
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String line = "";
		try {
			while ((line = bufferReader.readLine()) != null) {
				result.append(line); // reading the reply in HTML form and appending to the result
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parseResult(){
		Document doc = Jsoup.parse(result.toString()); // parsing the HTML contents in result 
		Elements headings = doc.select( "h1,h2,h3,h4,h5,h6" ); // selecting all heading elements 
		Elements paragraph = doc.select( "p" ); // selecting element paragraph
		
		Pattern pattern = Pattern.compile("[a-zA-Z]+"); //using regex to get only allowed characters
		Matcher m1 = pattern.matcher(paragraph.text());  //creates a matcher that will matches pattern from the combined text of paragraph element
		Matcher m2 = pattern.matcher(headings.text());
	
		while (m1.find()) {
			String tmp=m1.group();// getting words from paragraphs from matcher
			tmp=tmp.toLowerCase(); 
			if(!wordCount.containsKey(tmp)) //adding the word to TreeMap if not present
				wordCount.put(tmp, 1);
			else
				wordCount.put(tmp, wordCount.get(tmp)+1);// updating frequencies of words
	      
	    }
		while (m2.find()) {
			String tmp=m2.group();//getting words from headings
			tmp=tmp.toLowerCase();
			if(!wordCount.containsKey(tmp))
				wordCount.put(tmp, 1);
			else
				wordCount.put(tmp, wordCount.get(tmp)+1);
	      
	    }
		
	}
	
	private List<Entry<String, Integer>> sortByValue(Map<String, Integer> wordMap){//method to sort words in descending value of frequencies
        
        Set<Entry<String, Integer>> set = wordMap.entrySet();
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(set);
        Collections.sort( list, new Comparator<Map.Entry<String, Integer>>(){
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        });
        return list;
    }
	
	public static void main(String[] args) {
		System.out.println("Enter 5 URL for counting word frequency: ");
		Crawler crawler= new Crawler();
		Scanner sc=new Scanner(System.in);
		
		for(int i=0;i<5;i++){
			crawler.url.add(sc.next());//scanning input URL from user
		}
		sc.close();
		System.out.println("Fetching data from URL and Parsing the results");
		long startTime = System.nanoTime();  //for checking end to end run time
		double[] times=new double[10];
		
		int i=0;
		for(String tmp:crawler.url){
			long st=System.nanoTime();//checking running time for each URL
			crawler.fetchData(tmp);
			times[i++]=(System.nanoTime()-st)/1000000000.0;		//calculating run time for each URL in seconds
		}

		crawler.parseResult(); // calling parsing method to parse the results from HTTP execute into TreeMap with each word frequency
		
		File file =new File("UrlResults.txt");  //File for storing the results 
		FileOutputStream fout = null;
		try {
			file.createNewFile();
			System.out.println("File UrlResults.txt created");
			fout=new FileOutputStream(file);
			BufferedWriter bufferWriter = new BufferedWriter(new OutputStreamWriter(fout));  //Writing to file
			bufferWriter.write("Top 10 highest frequency words are: ");
			bufferWriter.newLine();
			List<Entry<String, Integer>> list = crawler.sortByValue(crawler.wordCount); // sorting the TreeMap in descending order
			int count=10;  // count for top 10 prevalent words
			for(Map.Entry<String, Integer> entry:list){
				if(count==0)
					break;
				if(entry.getKey().length()>1){//ignoring single letters like a,'s
					bufferWriter.newLine();
					bufferWriter.write(entry.getKey()+"->"+entry.getValue()); //writing words to file
					count--;
				}
			}
			bufferWriter.newLine();
			bufferWriter.newLine();
			bufferWriter.write("Statistics for individual URL are:");
			bufferWriter.newLine();
			DecimalFormat df = new DecimalFormat("##.###");
			for(int j=0;j<5;j++){
				bufferWriter.newLine();
				bufferWriter.write("URL" + (j+1) +" fetched in " + df.format(times[j]) + " seconds" ); //individual URL statistics
			}
			bufferWriter.newLine();
			bufferWriter.newLine();
    	
			double elapsedTime = (System.nanoTime() - startTime)/1000000000.0;
			bufferWriter.write("Total end to end run executed in " + df.format(elapsedTime) + " seconds"); //Total run time
	    
			bufferWriter.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
