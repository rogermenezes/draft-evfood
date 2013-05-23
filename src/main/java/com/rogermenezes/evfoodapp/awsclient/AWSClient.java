package com.rogermenezes.evfoodapp.awsclient;

import java.io.File;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AWSClient
{

	AmazonS3 s3;

	public AWSClient()
	{
		s3 = new AmazonS3Client(new BasicAWSCredentials("AKIAJZ63BIWKZBI3QRPQ", "lFCXxTn388LHz3j8SxMPbvsInU1SnFu00iYW3fqO"));

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//Region usWest2 = Region.getRegion(Regions.DEFAULT_REGION);
		//s3.setRegion(usWest2);

		System.out.println("Listing buckets");
//        for (Bucket bucket : s3.listBuckets()) {
//            System.out.println(" - " + bucket.getName());
//        }
        System.out.println();

	}

	public void uploadFiletoS3(String filename)
	{
		//s3.putObject(new PutObjectRequest("evernotefood", "a1db686b-f03d-42ef-91e9-eb8eff8d604e", createSampleFile("a1db686b-f03d-42ef-91e9-eb8eff8d604e")));
		s3.putObject(new PutObjectRequest("evernotefood", filename, createSampleFile(filename)));
	}

	private static File createSampleFile(String string) {
		// TODO Auto-generated method stub
		String filename = "C:/evernote/" + string;

		return new File(filename);
	}

}
