import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;

// http://info.cern.ch/ 5

public class Crawler {

    public static final int HTTP_PORT = 80;
    public static final String HOOK_REF = "<a href=\"";
    public static final String BAD_REQUEST_LINE = "HTTP/1.1 400 Bad Request";

    LinkedList<URLDepthPair> notVisitedList;
    LinkedList<URLDepthPair> visitedList;

    // Глубина поиска
    int depth;

    // Конструктор
    public Crawler() {
        notVisitedList = new LinkedList<URLDepthPair>();
        visitedList = new LinkedList<URLDepthPair>();
    }


    // Точка входа
    public static void main(String[] args) {

        Crawler crawler = new Crawler();

        crawler.getFirstURLDepthPair(args);
        crawler.startParse();
        crawler.showResults();
    }

    public void startParse() {
        System.out.println("Parse is starting:\n");

        URLDepthPair nowPage = notVisitedList.getFirst();

        while (nowPage.getDepth() <= depth && !notVisitedList.isEmpty()) {
            Socket socket = null;

            try {
                socket = new Socket(nowPage.getHostName(), HTTP_PORT);
                System.out.println("Successfully connected to " + nowPage.getURL());

                try {
                    socket.setSoTimeout(5000);
                } catch (SocketException exc) {
                    System.err.println("SocketException: " + exc.getMessage());
                    moveURLPair(nowPage, socket);
                    continue;
                }

                CrawlerHelper.getInfoAboutUrl(nowPage.getURL(), true);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("GET " + nowPage.getPagePath() + " HTTP/1.1");
                out.println("Host: " + nowPage.getHostName());
                out.println("Connection: close");
                out.println("");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line = in.readLine();

                if (line.startsWith(BAD_REQUEST_LINE)) {
                    System.out.println("BAD REQUEST: " + line + "\n");

                    this.moveURLPair(nowPage, socket);
                    continue;
                }

                System.out.println("Start reading...");
                int strCount = 0;
                int strCount2 = 0;
                while (line != null) {
                    try {
                        line = in.readLine();
                        strCount += 1;

                        String url = CrawlerHelper.getURLFromHTMLTag(line);
                        if (url == null) continue;

                        if (url.startsWith("https://")) {
                            System.out.println(strCount2 + " --> " + strCount + " |  " + url + " --> https-refference\n");
                            continue;
                        }

                        if (url.startsWith("../")) {
                            String newUrl = CrawlerHelper.urlFromBackRef(nowPage.getURL(), url);
                            System.out.println(strCount2 + " --> " + strCount + " |  " + url + " --> " + newUrl + "\n");
                            this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
                        } else if (url.startsWith("http://")) {
                            String newUrl = CrawlerHelper.cutTrashAfterFormat(url);
                            System.out.println(strCount2 + " --> " + strCount + " |  " + url + " --> " + newUrl + "\n");
                            this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
                        } else {
                            String newUrl;
                            newUrl = CrawlerHelper.cutURLEndFormat(nowPage.getURL()) + url;
                            System.out.println(strCount2 + " --> " + strCount + " |  " + url + " --> " + newUrl + "\n");
                            this.createURlDepthPairObject(newUrl, nowPage.getDepth() + 1);
                        }

                        strCount2 += 1;
                    } catch (Exception e) {
                        break;
                    }
                }

                if (strCount == 1) System.out.println("No refs on this page");
                System.out.println("End reading...\n");

                System.out.println("The page had been closed\n");

            } catch (UnknownHostException e) {
                System.out.println("UnknownHostException: " + nowPage.getURL());
            } catch (IOException e) {

                e.printStackTrace();
            }

            // Перемещение сайта после просмотра в список просмотренных
            moveURLPair(nowPage, socket);

            if (!notVisitedList.isEmpty())
                nowPage = notVisitedList.getFirst();
        }
    }

    private void moveURLPair(URLDepthPair pair, Socket socket) {
        this.visitedList.addLast(pair);
        this.notVisitedList.removeFirst();

        if (socket == null) return;

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createURlDepthPairObject(String url, int depth) {

        URLDepthPair newURL = null;
        try {
            // Формироване нового объекта и добавление его в список
            newURL = new URLDepthPair(url, depth);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        notVisitedList.addLast(newURL);
    }

    public LinkedList<URLDepthPair> getVisitedSites() {
        return this.visitedList;
    }

    public LinkedList<URLDepthPair> getNotVisitedSites() {
        return this.notVisitedList;
    }

    public void showResults() {
        System.out.println("Results==============");

        System.out.println("Sites was canned:");
        int count = 1;
        for (URLDepthPair pair : visitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }
        System.out.println("Not visited sites:");
        count = 1;
        for (URLDepthPair pair : notVisitedList) {
            System.out.println(count + " |  " + pair.toString());
            count += 1;
        }

        System.out.println("===========");
    }


    public void getFirstURLDepthPair(String[] args) {
        CrawlerHelper help = new CrawlerHelper();

        // Чтение аргументов из командной строки
        URLDepthPair urlDepth = help.getURLDepthPairFromArgs(args);
        if (urlDepth == null) {
            System.out.println("Args are empty or have exception. Now you need to enter URL and depth manually!\n");
            // Получение ввода от пользователей
            urlDepth = help.getURLDepthPairFromInput();
        }

        // Получение и замена глубины
        this.depth = urlDepth.getDepth();
        urlDepth.setDepth(0);

        // Занесение в список
        notVisitedList.add(urlDepth);

        // Вывод первого объекта URLDepthPair
        System.out.println("First site: " + urlDepth.toString() + "\n");
    }
}