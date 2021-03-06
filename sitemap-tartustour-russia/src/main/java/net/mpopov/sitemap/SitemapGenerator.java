package net.mpopov.sitemap;

import generated.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.redfin.sitemapgenerator.WebSitemapGenerator;

public class SitemapGenerator
{
    private static final Logger LOGGER = Logger
            .getLogger(SitemapGenerator.class);

    private static final String DOMAIN = Configuration.getInstance()
            .getDomain();

    private static final String PORT = Configuration.getInstance().getPort();

    private static final String A_HREF_CSS_QUERY = "a[href]";

    private static Set<String> allUrl = new HashSet<String>();
    
    private static Set<String> Urls = new HashSet<String>();

    private static Queue<String> pageUrl = new LinkedList<String>();

    public static void main(String[] args) throws IOException,
            InterruptedException
    {
        System.setProperty("java.net.useSystemProxies", "true");
        breadthFirstSearch();
        deletePortFromUrl();
        createSiteMap();
    }

    private static void deletePortFromUrl()
    {
        for (String url : allUrl)
            Urls.add(url.replace(DOMAIN + PORT, DOMAIN));
    }

    private static void breadthFirstSearch() throws IOException,
            InterruptedException
    {
        List<String> allowablePatterns = Configuration.getInstance()
                .getPatterns().getPattern();

        int delaySplitSize = 1000 * Configuration.getInstance()
                .getDelaySplitSize();

        int delayBetweenPages = Configuration.getInstance()
                .getDelayBetweenPages();

        int batchSize = Configuration.getInstance().getBatchSize();

        pageUrl.offer(DOMAIN + PORT);

        int page = 0;

        while (!pageUrl.isEmpty())
        {
            String link = pageUrl.poll();
            if (!allUrl.contains(link))
            {
                LOGGER.info("link: " + link);

                if (++page % batchSize == 0)
                    TimeUnit.SECONDS.sleep(delayBetweenPages);

                Document document = null;
                try
                {
                    document = Jsoup.connect(link).timeout(delaySplitSize)
                            .get();
                }
                catch (UnknownHostException e)
                {
                    String message = "UnknownHostException for link: " + link;
                    LOGGER.error(message);
                    continue;
                }
                catch (HttpStatusException e)
                {
                    String message = "HttpStatusException for link: " + link;
                    LOGGER.error(message);
                    continue;
                }
                catch (SocketTimeoutException e)
                {
                    String message = "SocketTimeoutException for link: " + link;
                    LOGGER.error(message);
                    continue;
                }
                catch (ConnectException e)
                {
                    String message = "ConnectException for link: " + link;
                    LOGGER.error(message);
                    continue;
                }

                allUrl.add(link);

                Elements links = document.select(A_HREF_CSS_QUERY);

                for (Element pLink : links)
                {
                    String linkHref = pLink.attr("href");
                    if (linkHref != null)
                    {
                        boolean allowable = true;
                        for (String pattern : allowablePatterns)
                            if (!linkHref.matches(pattern))
                            {
                                allowable = false;
                                break;
                            }

                        if (allowable)
                            if (linkHref.startsWith(DOMAIN + PORT))
                                pageUrl.offer(linkHref);
                            else if (linkHref.startsWith(DOMAIN))
                                pageUrl.offer(linkHref.replace(DOMAIN, DOMAIN
                                        + PORT));
                            else
                                pageUrl.offer(DOMAIN + PORT + linkHref);
                    }
                }
            }
        }
    }

    private static void createSiteMap() throws MalformedURLException
    {
        if (!Urls.isEmpty())
        {
            File baseDir = new File(Configuration.getInstance()
                    .getTempDirectory());
            WebSitemapGenerator wsg = new WebSitemapGenerator(DOMAIN, baseDir);

            for (String url : Urls)
                wsg.addUrl(url);

            wsg.write();
            wsg.writeSitemapsWithIndex();
            LOGGER.info("CREATE SITEMAP.XML");
        }
        else
        {
            String message = "No URLs added, sitemap would be empty;"
                    + " you must add some URLs with addUrls.";
            LOGGER.error(message);
        }
    }
}
