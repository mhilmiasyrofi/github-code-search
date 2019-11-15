package com.project.githubsearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.kevinsawicki.http.HttpRequest;
import com.project.githubsearch.model.MavenPackage;
import com.project.githubsearch.model.Query;
import com.project.githubsearch.model.Response;
import com.project.githubsearch.model.SynchronizedData;
import com.project.githubsearch.utils.DirExplorer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * Github Search Engine
 *
 */
public class App {

    // run multiple token
    private static final String AUTH_TOKEN = System.getenv("GITHUB_AUTH_TOKEN");
    private static final String AUTH_TOKEN_2 = System.getenv("GITHUB_AUTH_TOKEN_2");
    private static int lastToken = 1;
  
    // parameter for the request
    private static final String PARAM_QUERY = "q"; //$NON-NLS-1$
    private static final String PARAM_PAGE = "page"; //$NON-NLS-1$
    private static final String PARAM_PER_PAGE = "per_page"; //$NON-NLS-1$
    
    // links from the response header
    private static final String META_REL = "rel"; //$NON-NLS-1$
    private static final String META_NEXT = "next"; //$NON-NLS-1$
    private static final String DELIM_LINKS = ","; //$NON-NLS-1$
    private static final String DELIM_LINK_PARAM = ";"; //$NON-NLS-1$

    // response code from github
    private static final int BAD_CREDENTIAL = 401;
    private static final int RESPONSE_OK = 200;
    private static final int ABUSE_RATE_LIMITS = 403;
    private static final int UNPROCESSABLE_ENTITY = 422;

    private static final int NUMBER_THREADS = 4;
    private static final long INFINITY = -1;
    private static long MAX_DATA = INFINITY;

    // folder location to save the downloaded files and jars
    private static final String DATA_LOCATION = "src/main/java/com/project/githubsearch/data/";
    private static final String FILES_LOCATION = "src/main/java/com/project/githubsearch/files/";
    private static final String JARS_LOCATION = "src/main/java/com/project/githubsearch/jars/";

    private static SynchronizedData synchronizedData = new SynchronizedData();

    public static void main(String[] args) {
        Query query = new Query();
        Scanner scanner = new Scanner(System.in);
        boolean inputFinish = false;
        do {
            System.out.println("Enter the query: ");
            String stringQuery = scanner.nextLine();
            query = parseQuery(stringQuery);
            if (!query.getMethod().equals("")){
                inputFinish = true;
            }
        } while (!inputFinish);
        scanner.close();
        System.out.println("Query: " + query.toString());

        MAX_DATA = 100;

        searchCode(query);

        List<File> files = findJavaFiles(new File(FILES_LOCATION + query.getMethod().toString() + "/"));
        for (File file : files) {
            processJavaFile(file, query);
        }
    }

    private static ArrayList<Query> inputQuery(){
        ArrayList<Query> queries = new ArrayList<Query>();
        Scanner scanner = new Scanner(System.in);
        boolean inputFinish = false;
        do {
            System.out.println("Enter the query: ");
            String stringQuery = scanner.nextLine();
            Query query = parseQuery(stringQuery);
            if (!query.getMethod().equals("")) {
                System.out.println("Do you want to search multiple queries? y/n");
                String isMultipleQuery = scanner.nextLine();
                if (!isMultipleQuery.equals("y")){
                    inputFinish = true;
                }
            }
        } while (!inputFinish);
        scanner.close();
        System.out.println("Queries: " + queries.toString());

        return queries;
    }


    private static Query parseQuery(String s) {
        Query query = new Query();

        s = s.replace(" ", "");
        int leftBracketLocation = s.indexOf('(');
        int rightBracketLocation = s.indexOf(')');
        if (leftBracketLocation == -1 || rightBracketLocation == -1) {
            System.out.println("Your query isn't accepted");
            System.out.println("Query Format: " + "method(argument_1, argument_2, ... , argument_n)");
            System.out.println("Example: " + "addAction(int, java.lang.CharSequence, android.app.PendingIntent)");
            return query;
        } else {
            String method = s.substring(0, leftBracketLocation);
            String args = s.substring(leftBracketLocation + 1, rightBracketLocation);
            String[] arr = args.split(",");
            ArrayList<String> arguments = new ArrayList<String>();
            for (int i = 0; i < arr.length; i++) {
                arguments.add(arr[i]);
            }
            query.setMethod(method);
            query.setArguments(arguments);
        }
        return query;
    }

    private static void searchCode(Query query) {
        
        File dataFolder = new File(DATA_LOCATION);
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        // path to save the github code response
        String pathToSaveGithubResponse = DATA_LOCATION + query.getMethod().toString() + ".json";
        getData(query, pathToSaveGithubResponse);
        downloadData(query, pathToSaveGithubResponse);
    }

    private static void getData(Query query, String pathFile) {
        String stringQuery = query.getMethod();
        for (int i = 0; i < query.getArguments().size(); i++) {
            stringQuery += " " + query.getArguments().get(i);
        }

        String endpoint = "https://api.github.com/search/code";

        final int MAX_SIZE = 384000; // the max searchable size from github api
        final int TOTAL_COUNT_LIMIT = 1000;
        int INITIAL_INTERVAL = 2048;
        int lower_bound = 0;
        int total_count = 0;
        int dynamic_interval, upper_bound;

        int per_page_limit = 100;
        int page = 1;

        System.out.println("Getting data from Github");
        System.out.println("with per page limit: " + per_page_limit);

        Response firstResponse = handleCustomGithubRequest(endpoint, stringQuery, 0, MAX_SIZE, page, per_page_limit);
        System.out.println();
        System.out.println("Request: " + firstResponse.getUrlRequest());
        System.out.println("Total items from github: " + firstResponse.getTotalCount());

        if (MAX_DATA != INFINITY) {
            System.out.println("Not downloading all data. Just " + MAX_DATA);
        }
        System.out.println();

        if (firstResponse.getTotalCount() < 1000) {

            Response response = firstResponse;
            JSONArray item = response.getItem();
            System.out.println("Request: " + response.getUrlRequest());
            System.out.println("Number items: " + item.length());
            synchronizedData.addArray(item);

            int lastPage = (int) Math.ceil(firstResponse.getTotalCount() / 100.0);

            ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);
            for (int j = 2; j <= lastPage; j++) {
                page = j;
                Runnable worker = new URLRunnable(endpoint, stringQuery, 0, MAX_SIZE, page, per_page_limit);
                executor.execute(worker);
            }

            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {
            }

        } else {
            dynamic_interval = INITIAL_INTERVAL;
            Response response = new Response();
            while (lower_bound < MAX_SIZE) {
                page = 1;
                upper_bound = lower_bound + dynamic_interval;
                response = handleCustomGithubRequest(endpoint, stringQuery, lower_bound, upper_bound, page, per_page_limit);
                total_count = response.getTotalCount();

                if (total_count < TOTAL_COUNT_LIMIT) { // create the dynamic range higher
                    System.out.println("Create the dynamic interval higher");
                    while (total_count < 200 && upper_bound < MAX_SIZE) {
                        dynamic_interval = dynamic_interval * 2;
                        upper_bound = lower_bound + dynamic_interval;
                        int prev_total_count = total_count;
                        response = handleCustomGithubRequest(endpoint, stringQuery, lower_bound, upper_bound, page,
                                per_page_limit);
                        total_count = response.getTotalCount();
                        if (total_count > TOTAL_COUNT_LIMIT) {
                            total_count = prev_total_count;
                            dynamic_interval = (int) dynamic_interval / 2;
                            upper_bound = lower_bound + dynamic_interval;
                        }
                        System.out.println("Dynamic interval: " + dynamic_interval);
                    }
                } else { // create the dynamic interval smaller
                    System.out.println("Create the dynamic interval smaller");
                    do {
                        dynamic_interval = (int) dynamic_interval / 2;
                        upper_bound = lower_bound + dynamic_interval;
                        response = handleCustomGithubRequest(endpoint, stringQuery, lower_bound, upper_bound, page,
                                per_page_limit);
                        total_count = response.getTotalCount();
                        System.out.println("Dynamic interval: " + dynamic_interval);
                    } while (total_count > TOTAL_COUNT_LIMIT && dynamic_interval > 1);
                }

                System.out.println("");
                System.out.println("Lower bound: " + lower_bound);
                System.out.println("Upper bound: " + upper_bound);
                System.out.println("Total count in this range size: " + total_count);

                JSONArray item = response.getItem();
                System.out.println("Request: " + response.getUrlRequest());
                System.out.println("Number items: " + item.length());
                synchronizedData.addArray(item);

                int lastPage = (int) Math.ceil(total_count / 100.0);

                ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);

                for (int j = 2; j <= lastPage; j++) {
                    page = j;
                    Runnable worker = new URLRunnable(endpoint, stringQuery, lower_bound, upper_bound, page, per_page_limit);
                    executor.execute(worker);
                }

                executor.shutdown();
                // Wait until all threads are finish
                while (!executor.isTerminated()) {
                }

                lower_bound = upper_bound;

                if (MAX_DATA != INFINITY) {
                    if (synchronizedData.getData().length() > MAX_DATA) {
                        break;
                    }
                }
            }
        }

        System.out.println("\nTotal items for all requests: " + synchronizedData.getData().length());

        try {
            Writer output = null;
            File file = new File(pathFile);
            output = new BufferedWriter(new FileWriter(file));
            output.write(synchronizedData.getData().toString());
            output.close();
        } catch (IOException e) {
            System.out.println("\nEXCEPTION");
            System.out.println("Can't save the data to external file!");
            System.exit(-1);
        }
    }

    public static class URLRunnable implements Runnable {
        private final String url;

        URLRunnable(String endpoint, String query, int lower_bound, int upper_bound, int page, int per_page_limit) {
            upper_bound++;
            lower_bound--;
            String size = lower_bound + ".." + upper_bound; // lower_bound < size < upper_bound
            this.url = endpoint + "?" + PARAM_QUERY + "=" + query + "+in:file+language:java+extension:java+size:" + size
                    + "&" + PARAM_PAGE + "=" + page + "&" + PARAM_PER_PAGE + "=" + per_page_limit;
        }

        @Override
        public void run() {
            Response response = handleGithubRequestWithUrl(url);
            JSONArray item = response.getItem();
            System.out.println("Request: " + response.getUrlRequest());
            System.out.println("Number items: " + item.length());
            synchronizedData.addArray(item);
        }
    }

    private static void downloadData(Query query, String pathToData) {
        File files = new File(FILES_LOCATION);
        if (!files.exists()){
            files.mkdir();
        }
        File exactFolder = new File(FILES_LOCATION + query.getMethod().toString() + "/");
        if (!exactFolder.exists()){
            exactFolder.mkdir();
        }
        try {
            Stream<String> lines = Files.lines(Paths.get(pathToData));
            String content = lines.collect(Collectors.joining(System.lineSeparator()));

            // parse json array
            JSONArray items = new JSONArray(content);
            
            long n = items.length();
            if (MAX_DATA != INFINITY) n = MAX_DATA;

            for (int it = 0; it < n; it++) {
                JSONObject item = new JSONObject(items.get(it).toString());
                String html_url = item.getString("html_url");
                int id = item.getInt("id");

                // convert html url to downloadable url
                // based on my own analysis
                String download_url = convertHTMLUrlToDownloadUrl(html_url);

                String[] parts = html_url.split("/");
                // using it to make a unique name
                // replace java to txt for excluding from maven builder
                String fileName = id + "_" + parts[parts.length - 1].replace(".java", ".txt");

                System.out.println();
                System.out.println("Downloading the file: " + (it + 1));
                System.out.println("HTML Url: " + html_url);

                try {
                    // download file from url
                    URL url;
                    url = new URL(download_url);
                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                    String pathFile = new String(FILES_LOCATION + query.getMethod().toString() +  "/" + fileName);
                    FileOutputStream fileOutputStream = new FileOutputStream(pathFile);
                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    System.out.println("Can't download the github file");
                    System.out.println("File not found!");
                } catch (MalformedURLException e) {
                    System.out.println("Malformed URL Exception");
                    e.printStackTrace();
                }
            }

            lines.close();
        } catch (IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
        }
    }

    /**
     * Convert github html url to download url input:
     * https://github.com/shuchen007/ThinkInJavaMaven/blob/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
     * output:
     * https://raw.githubusercontent.com/shuchen007/ThinkInJavaMaven/85b402a81fc0b3f2f039b34c23be416322ef14b4/src/main/java/sikaiClient/IScyxKtService.java
     */
    private static String convertHTMLUrlToDownloadUrl(String html_url) {
        String[] parts = html_url.split("/");
        String download_url = "https://raw.githubusercontent.com/";
        int l = parts.length;
        for (int i = 0; i < l; i++) {
            if (i >= 3) {
                if (i != 5) {
                    if (i != l - 1) {
                        download_url = download_url.concat(parts[i] + '/');
                    } else {
                        download_url = download_url.concat(parts[i]);
                    }
                }
            }
        }

        return download_url;
    }

    private static Response handleGithubRequestWithUrl(String url) {

        boolean response_ok = false;
        Response response = new Response();
        int responseCode;

        do {

            HttpRequest request;

            // encode the space into %20
            url = url.replace(" ", "%20");

            if (lastToken == 1) {
                lastToken = 2;
                request = HttpRequest.get(url, false).authorization("token " + AUTH_TOKEN);
            } else {
                lastToken = 1;
                request = HttpRequest.get(url, false).authorization("token " + AUTH_TOKEN_2);
            }

            // System.out.println("Request: " + request);

            // handle response
            responseCode = request.code();
            if (responseCode == RESPONSE_OK) {
                // System.out.println("Header: " + request.headers());
                response.setCode(responseCode);
                JSONObject body = new JSONObject(request.body());
                response.setTotalCount(body.getInt("total_count"));
                response.setItem(body.getJSONArray("items"));
                response.setUrlRequest(request.toString());
                response.setNextUrlRequest(getNextLinkFromResponse(request.header("Link")));
                response_ok = true;
            } else if (responseCode == BAD_CREDENTIAL) {
                System.out.println("Authorization problem");
                System.out.println("Please read the readme file!");
                System.out.println("Github message: " + (new JSONObject(request.body())).getString("message"));
                System.exit(-1);
            } else if (responseCode == ABUSE_RATE_LIMITS) {
                System.out.println("Abuse Rate Limits");
                // retry current progress after wait for a minute
                String retryAfter = request.header("Retry-After");
                try {
                    int sleepTime = 0; // wait for a while
                    if (retryAfter.isEmpty()) {
                        sleepTime = 1;
                    } else {
                        sleepTime = new Integer(retryAfter).intValue();
                    }
                    System.out.println("Retry-After: " + sleepTime);
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (responseCode == UNPROCESSABLE_ENTITY) {
                System.out.println("Response Code: " + responseCode);
                System.out.println("Unprocessbale Entity: only the first 1000 search results are available");
                System.out.println("See the documentation here: https://developer.github.com/v3/search/");
            } else {
                System.out.println("Response Code: " + responseCode);
                System.out.println("Response Body: " + request.body());
                System.out.println("Response Headers: " + request.headers());
                System.exit(-1);
            }

        } while (!response_ok && responseCode != UNPROCESSABLE_ENTITY);

        return response;
    }

    private static Response handleCustomGithubRequest(String endpoint, String query, int lower_bound, int upper_bound,
            int page, int per_page_limit) {
        // The size range is exclusive
        upper_bound++;
        lower_bound--;
        String size = lower_bound + ".." + upper_bound; // lower_bound < size < upper_bound

        String url;
        Response response = new Response();

        url = endpoint + "?" + PARAM_QUERY + "=" + query
                + "+in:file+language:java+extension:java+size:" + size + "&" + PARAM_PAGE + "=" + page + "&"
                + PARAM_PER_PAGE + "=" + per_page_limit;
        response = handleGithubRequestWithUrl(url);

        return response;
    }
    private static String getNextLinkFromResponse(String linkHeader) {

        String next = null;

        if (linkHeader != null) {
            String[] links = linkHeader.split(DELIM_LINKS);
            for (String link : links) {
                String[] segments = link.split(DELIM_LINK_PARAM);
                if (segments.length < 2)
                    continue;

                String linkPart = segments[0].trim();
                if (!linkPart.startsWith("<") || !linkPart.endsWith(">")) //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                linkPart = linkPart.substring(1, linkPart.length() - 1);

                for (int i = 1; i < segments.length; i++) {
                    String[] rel = segments[i].trim().split("="); //$NON-NLS-1$
                    if (rel.length < 2 || !META_REL.equals(rel[0]))
                        continue;

                    String relValue = rel[1];
                    if (relValue.startsWith("\"") && relValue.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
                        relValue = relValue.substring(1, relValue.length() - 1);

                    if (META_NEXT.equals(relValue))
                        next = linkPart;
                }
            }
        }
        return next;
    }

    private static void printSign(String s, int x) {
        for (int i = 0; i < x; i++) {
            System.out.print(s);
        }
        System.out.println();
    }

    private static void processJavaFile(File file, Query query) {

        System.out.println();
        System.out.println();
        printSign("=", file.toString().length() + 6);
        System.out.println("File: " + file);
        printSign("=", file.toString().length() + 6);

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(false),
                new JavaParserTypeSolver(new File("src/main/java")));
        List<String> addedJars = getNeededJars(file);
        for (int i = 0; i < addedJars.size(); i++) {
            try {
                TypeSolver jarTypeSolver = JarTypeSolver.getJarTypeSolver(addedJars.get(i));
                combinedTypeSolver.add(jarTypeSolver);
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println("Package corrupt!");
                System.out.println("Corrupted Jars: " + addedJars.get(i));
            }
        }

        // System.out.println();
        // System.out.println("=== Resolving ===");
        try {
            StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
            CompilationUnit cu;
            cu = StaticJavaParser.parse(file);
            cu.findAll(MethodCallExpr.class).forEach(mce -> {
                if (mce.getName().toString().equals(query.getMethod()) && mce.getArguments().size() == query.getArguments().size()) {
                    try {
                        ResolvedMethodDeclaration resolvedMethodDeclaration = mce.resolve();
                        boolean isArgumentTypeMatch = true;
                        for (int i = 0; i < resolvedMethodDeclaration.getNumberOfParams(); i++) {
                            if (!query.getArguments().get(i).equals(resolvedMethodDeclaration.getParam(i).describeType())){
                                isArgumentTypeMatch = false;
                                break;
                            }
                        }
                        System.out.println();
                        if (isArgumentTypeMatch) {
                            System.out.println("+++ Resolving Success - Match Arguments +++");
                        } else {
                            System.out.println("+++ Resolving Success - Arguments don't match +++");
                        }
                        System.out.println("Statement: " + mce);
                        System.out.println("Method: " + resolvedMethodDeclaration.getName());
                        System.out.println("Type:" + resolvedMethodDeclaration.getPackageName() + "."
                                + resolvedMethodDeclaration.getClassName());
                        System.out.println("Number of Arguments: " + resolvedMethodDeclaration.getNumberOfParams());
                        System.out.println("Arguments: " + mce.getArguments());
                        for (int i = 0; i < resolvedMethodDeclaration.getNumberOfParams(); i++) {
                            System.out.println("Argument " + (i+1) + " type: "
                                    + resolvedMethodDeclaration.getParam(i).describeType());
                        }
                        System.out.println("Location:" + mce.getBegin().get());
                    } catch (Exception e) {
                        System.out.println();
                        System.out.println("--- Resolving Fail ---");
                        System.out.println("Error metod: " + mce);
                        // e.printStackTrace();
                    }
                }
            });
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (ParseProblemException parseProblemException) {
            System.out.println("Parse Problem Exception");
        }
    }

    private static List<File> findJavaFiles(File src) {
        List<File> files = new LinkedList<File>();
        new DirExplorer((level, path, file) -> path.endsWith(".txt"), (level, path, file) -> {
            files.add(file);
        }).explore(src);

        return files;
    }

    private static List<String> getNeededJars(File file) {
        List<String> jarsPath = new ArrayList<String>();
        TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(false),
                new JavaParserTypeSolver(new File("src/main/java")));
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));

        // list of specific package imported
        List<String> importedPackages = new ArrayList<String>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            cu.findAll(Name.class).forEach(mce -> {
                String[] names = mce.toString().split("[.]");
                if (names.length >= 2) { //filter some wrong detected import like Override, SupressWarning
                    if (importedPackages.isEmpty()) {
                        importedPackages.add(mce.toString());
                    } else {
                        boolean isAlreadyDefined = false;
                        for (int i = 0; i < importedPackages.size(); i++) {
                            if (importedPackages.get(i).contains(mce.toString())) {
                                isAlreadyDefined = true;
                                break;
                            }
                        }
                        if (!isAlreadyDefined) {
                            importedPackages.add(mce.toString());
                        }
                    }
                }
            });
        } catch (FileNotFoundException e) {
            System.out.println("EXCEPTION");
            System.out.println("File not found!");
        } catch (ParseProblemException parseException) {
            return jarsPath;
        }

        // System.out.println();
        // System.out.println("=== Imported Packages ==");
        // for (int i = 0; i < importedPackages.size(); i++) {
        //     System.out.println(importedPackages.get(i));
        // }
        
        // filter importedPackages
        // remove the project package and java predefined package
        List<String> neededPackages = new ArrayList<String>();
        if (importedPackages.size() > 0) {
            String qualifiedName = importedPackages.get(0);
            String[] names = qualifiedName.split("[.]");
            String projectPackage = names[0].toString();
            for (int i = 1; i < importedPackages.size(); i++) { // the first package is skipped
                qualifiedName = importedPackages.get(i);
                names = qualifiedName.split("[.]");
                String basePackage = names[0];
                if (!basePackage.equals(projectPackage) && !basePackage.equals("java")
                && !basePackage.equals("javax") && !basePackage.equals("Override")) {
                    neededPackages.add(importedPackages.get(i));
                }
            }
        }
        
        // System.out.println();
        // System.out.println("=== Needed Packages ==");
        // for (int i = 0; i < neededPackages.size(); i++) {
        //     System.out.println(neededPackages.get(i));
        // }

        List<MavenPackage> mavenPackages = new ArrayList<MavenPackage>();

        // get the groupId and artifactId from the package qualified name
        for (int i = 0; i < neededPackages.size(); i++) {
            String qualifiedName = neededPackages.get(i);
            MavenPackage mavenPackage = getMavenPackageArtifact(qualifiedName);

            if (!mavenPackage.getId().equals("")) { // handle if the maven package is not exist
                // filter if the package is used before
                boolean isAlreadyUsed = false;
                for (int j = 0; j < mavenPackages.size(); j++) {
                    MavenPackage usedPackage = mavenPackages.get(j);
                    if (mavenPackage.getGroupId().equals(usedPackage.getGroupId())
                            && mavenPackage.getArtifactId().equals(usedPackage.getArtifactId())) {
                        isAlreadyUsed = true;
                    }
                }
                if (!isAlreadyUsed) {
                    mavenPackages.add(mavenPackage);
                }
            }
        }

        // System.out.println();
        // System.out.println("=== Maven Packages ==");
        // for (int i = 0; i < mavenPackages.size(); i++) {
        //     System.out.println("GroupID: " + mavenPackages.get(i).getGroupId() + " - ArtifactID: "
        //             + mavenPackages.get(i).getArtifactId());
        // }

        File jarFolder = new File(JARS_LOCATION);
        if (!jarFolder.exists()) {
            jarFolder.mkdir();
        }
        
        // System.out.println();
        // System.out.println("=== Downloading Packages ==");
        for (int i = 0; i < mavenPackages.size(); i++) {
            String pathToJar = downloadMavenJar(mavenPackages.get(i).getGroupId(),
                    mavenPackages.get(i).getArtifactId());
            if (!pathToJar.equals("")) {
                // System.out.println("Downloaded: " + pathToJar);
                jarsPath.add(pathToJar);
            }
        }

        return jarsPath;
    }

    // download the latest package by groupId and artifactId
    private static String downloadMavenJar(String groupId, String artifactId) {
        String path = JARS_LOCATION + artifactId + "-latest.jar";
        String url = "http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=" + groupId
                + "&a=" + artifactId + "&v=LATEST";
        // System.out.println("URL: " + url);
        File jarFile = new File(path);

        if (!jarFile.exists()) {
            // Equivalent command conversion for Java execution
            String[] command = { "curl", "-L", url, "-o", path };

            ProcessBuilder process = new ProcessBuilder(command);
            Process p;
            try {
                p = process.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                String result = builder.toString();
                System.out.print(result);

            } catch (IOException e) {
                System.out.print("error");
                e.printStackTrace();
            }
        }

        return path;

    }

    private static MavenPackage getMavenPackageArtifact(String qualifiedName) {

        MavenPackage mavenPackageName = new MavenPackage();

        String url = "https://search.maven.org/solrsearch/select?q=fc:" + qualifiedName + "&wt=json";

        HttpRequest request = HttpRequest.get(url, false);

        // handle response
        int responseCode = request.code();
        if (responseCode == RESPONSE_OK) {
            JSONObject body = new JSONObject(request.body());
            JSONObject response = body.getJSONObject("response");
            int numFound = response.getInt("numFound");
            JSONArray mavenPackages = response.getJSONArray("docs");
            if (numFound > 0) {
                mavenPackageName.setId(mavenPackages.getJSONObject(0).getString("id")); // set the id
                mavenPackageName.setGroupId(mavenPackages.getJSONObject(0).getString("g")); // set the first group id
                mavenPackageName.setArtifactId(mavenPackages.getJSONObject(0).getString("a")); // set the first artifact id
                mavenPackageName.setVersion(mavenPackages.getJSONObject(0).getString("v")); // set the first version id
            }
        } else {
            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Body: " + request.body());
            System.out.println("Response Headers: " + request.headers());
        }

        return mavenPackageName;
    }

}
