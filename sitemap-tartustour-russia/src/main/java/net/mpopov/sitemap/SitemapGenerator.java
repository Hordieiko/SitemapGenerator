package net.mpopov.sitemap;

import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.redfin.sitemapgenerator.WebSitemapGenerator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import generated.Configuration;

public class SitemapGenerator
{
    private static final Logger LOGGER = Logger
            .getLogger(SitemapGenerator.class);

    private static final String DOMAIN = Configuration.getInstance()
            .getDomain();

    private static final String A_HREF_CSS_QUERY = "a[href]";

    private static Set<String> garbage = new HashSet<String>();

    private static Set<String> usedUrl = new HashSet<String>();

    private static ArrayList<String> allUrl = new ArrayList<String>();

    public static void main(String[] args) throws IOException,
            InterruptedException
    {
        allUrl.add(DOMAIN);

        breadthFirstSearch();

        allUrl.removeAll(garbage);

        Set<String> Result = new HashSet<String>(allUrl);

        createSiteMap(Result);
    }

    private static void breadthFirstSearch() throws IOException,
            InterruptedException
    {
        List<String> allowablePatterns = Configuration.getInstance()
                .getPatterns().getPattern();

        int delaySplitSize = 1000 * Configuration.getInstance().getDelaySplitSize();

        int delayBetweenPages = Configuration.getInstance()
                .getDelayBetweenPages();

        int batchSize = Configuration.getInstance().getBatchSize();

        ListIterator<String> iter = allUrl.listIterator();
        while (iter.hasNext())
        {
            String link = iter.next();
            if (!usedUrl.contains(link))
            {
                if (iter.nextIndex() % batchSize == 0)
                    TimeUnit.SECONDS.sleep(delayBetweenPages);

                usedUrl.add(link);

                Document document = null;
                try
                {
                    document = Jsoup.connect(link).timeout(delaySplitSize)
                            .get();
                }
                catch (HttpStatusException e)
                {
                    String message = String
                            .format("HttpStatusException for link: " + link);
                    LOGGER.error(message);
                    garbage.add(link);
                    continue;
                }
                catch (SocketTimeoutException e)
                {
                    String message = String
                            .format("SocketTimeoutException for link: " + link);
                    LOGGER.error(message);
                    garbage.add(link);
                    continue;
                }

                Elements links = document.select(A_HREF_CSS_QUERY);

                for (Element pLink : links)
                {
                    String linkHref = pLink.attr("href");
                    if (linkHref != null && !allUrl.contains(linkHref)
                            && !allUrl.contains(DOMAIN + linkHref))
                    {
                        boolean allowable = true;
                        for (String pattern : allowablePatterns)
                            if (linkHref.matches(pattern))
                                allowable = false;

                        if (allowable)
                        {
                            if (linkHref.contains(DOMAIN))
                                iter.add(linkHref);
                            else
                                iter.add(DOMAIN + linkHref);
                            iter.previous();
                        }
                    }
                }
            }
        }
        return;
    }

    private static void createSiteMap(Set<String> Result)
            throws MalformedURLException
    {
        if (!Result.isEmpty())
        {
            File baseDir = new File(Configuration.getInstance()
                    .getTempDirectory());
            WebSitemapGenerator wsg = new WebSitemapGenerator(DOMAIN, baseDir);

            for (String url : Result)
                wsg.addUrl(url);

            wsg.write();
            wsg.writeSitemapsWithIndex();
            LOGGER.info("CREATE SITEMAP.XML");
        }
        else
        {
            String message = String
                    .format("No URLs added, sitemap would be empty;"
                            + " you must add some URLs with addUrls.");
            LOGGER.error(message);
        }
        return;
    }
}
