package org.markmal.fanera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.markmal.fanera.GitHubReleaseChecker.GitHubRelease;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class GitHubReleaseChecker {

  public class GitHubRelease {
    String url;
    String html_url;
    String id;
    String tag_name;
    String name;
    String prerelease;
    String created_at;
    String published_at;
  }

	
	static Type GitHubReleasesType = new TypeToken<Collection<GitHubRelease>>(){}.getType();
	
	Collection<GitHubRelease> releasesCollection;
	String urlString;

	public GitHubReleaseChecker(String url) {
		this.urlString = url;
		try {
			releasesCollection = load(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String readUrl(String urlString) throws IOException  {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	Collection<GitHubRelease> load(String url) throws IOException {
		String json = readUrl(url);
		Gson gson = new Gson();        
		Collection<GitHubRelease> r = gson.fromJson(json, GitHubReleasesType);
	    return r;
	}
	
	// 1 when rn2>rn1, -1 when rn2<rn1, 0 when equal 
	int compareReleaseNumbers(int[] rn1, int[] rn2) {
		for (int i=0; i<rn1.length; i++) {
			if(rn2[i] > rn1[i]) return 1;
			if(rn2[i] < rn1[i]) return -1;
		}
		return 0;
	}
	
	int[] tagToNumbers(String tag) {
		String[] rns = tag.split("\\.");
		int[] rn = new int[rns.length]; 
		for (int i=0; i<rns.length; i++)
			rn[i] = new Integer(rns[i]); 
		return rn; 
	}

	int compareReleaseStrings(String s1, String s2) {
		int[] rn1 = tagToNumbers(s1);
		int[] rn2 = tagToNumbers(s2);
		return compareReleaseNumbers(rn1,rn2);
	}

	GitHubRelease findLatestRelease() {
		int[] rel_nums = {0,0,0,0};
		GitHubRelease last_ghr = null;
		for(GitHubRelease ghr : releasesCollection) {
			int[] rn = tagToNumbers(ghr.tag_name);
			if (compareReleaseNumbers(rel_nums,rn)==1) {
				rel_nums[0] = rn[0];
				rel_nums[1] = rn[1];
				rel_nums[2] = rn[2];
				rel_nums[3] = rn[3];
				last_ghr = ghr;
			}
		}
		return last_ghr;
	}
	
}
