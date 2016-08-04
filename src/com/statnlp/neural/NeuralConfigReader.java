package com.statnlp.neural;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class NeuralConfigReader {
	public static void readConfig(String filename) throws FileNotFoundException {
		Scanner scan = new Scanner(new File(filename));
		while(scan.hasNextLine()){
			String line = scan.nextLine().trim();
			if(line.equals("")){
				continue;
			}
			String[] info = line.split(" ");
			if(info[0].equals("serverPort")) {
				NeuralConfig.NEURAL_SERVER_PORT= Integer.parseInt(info[1]);
			} else if(info[0].equals("wordEmbedding")) {
				NeuralConfig.WORD_EMBEDDING_SIZE = Integer.parseInt(info[1]);
			} else if(info[0].equals("numLayer")) {
				NeuralConfig.NUM_LAYER = Integer.parseInt(info[1]);
			} else if(info[0].equals("hiddenSize")) {
				NeuralConfig.HIDDEN_SIZE = Integer.parseInt(info[1]);
			} else if(info[0].equals("activation")) {
				NeuralConfig.ACTIVATION = info[1];
			} else if(info[0].equals("dropout")) {
				NeuralConfig.DROPOUT = Double.parseDouble(info[1]);
			} else if(info[0].equals("serverAddress")) {
				NeuralConfig.NEURAL_SERVER_ADDRESS = info[1];
			} else {
				System.err.println("Unrecognized option: " + line);
			}
		}
		
		scan.close();
	}
}
