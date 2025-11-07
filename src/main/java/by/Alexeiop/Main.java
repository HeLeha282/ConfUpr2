package by.Alexeiop;

import java.sql.SQLOutput;
import java.util.HashSet;
import java.util.Set;

public class Main {


    public static void main(String[] args) {
        String nameAnalysisPackage = null;
        String urlAddressRepository = null;
        String workingModeWithTestRepository = null;
        String versionPackage = null;
        String nameFileWithImageGraph = null;
        String ModePrintDependency = null;
        Integer maxDepthAnalysisDependencies = null;

        System.out.println();

        Set<String> paramSet = Set.of("-nameAnalysisPackage", "-urlAddressRepository", "-workingModeWithTestRepository",
                "-versionPackage", "-nameFileWithImageGraph", "-ModePrintDependency", "-maxDepthAnalysisDependencies");


        for (int i = 0; i < args.length-1; i++) {
            if (args[i].equals("-nameAnalysisPackage")) {
                if (!paramSet.contains(args[++i])) {
                    nameAnalysisPackage = args[i];
                }
            }
            if (args[i].equals("-urlAddressRepository")) {
                if (!paramSet.contains(args[++i])) {
                    urlAddressRepository = args[i];
                }
            }
            if (args[i].equals("-workingModeWithTestRepository")) {
                if (!paramSet.contains(args[++i])) {
                    workingModeWithTestRepository = args[i];
                }
            }
            if (args[i].equals("-versionPackage")) {
                if (!paramSet.contains(args[++i])) {
                    versionPackage = args[i];
                }
            }
            if (args[i].equals("-nameFileWithImageGraph")) {
                if (!paramSet.contains(args[++i])) {
                    nameFileWithImageGraph = args[i];
                }
            }
            if (args[i].equals("-ModePrintDependency")) {
                if (!paramSet.contains(args[++i])) {
                    ModePrintDependency = args[i];
                }
            }
            if (args[i].equals("-maxDepthAnalysisDependencies")) {
                if (!paramSet.contains(args[++i])) {
                    maxDepthAnalysisDependencies = Integer.valueOf(args[i]);
                }
            }
        }

        System.out.println(
                "nameAnalysisPackage='" + nameAnalysisPackage + "\'," + '\n' +
                        "urlAddressRepository='" + urlAddressRepository + "\'," + '\n' +
                        "workingModeWithTestRepository='" + workingModeWithTestRepository + "\'," + '\n' +
                        "versionPackage='" + versionPackage + "\'," + '\n' +
                        "nameFileWithImageGraph='" + nameFileWithImageGraph + "\'," + '\n' +
                        "ModePrintDependency='" + ModePrintDependency + "\'," + '\n' +
                        "maxDepthAnalysisDependencies=" + maxDepthAnalysisDependencies);

        System.out.println();
    }


}