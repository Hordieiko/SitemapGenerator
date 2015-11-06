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
import java.util.ListIterator;
import java.util.Set;

import generated.Configuration;

public class SitemapGenerator
{
    private static final Logger logger = Logger
            .getLogger(SitemapGenerator.class);

    private static final String DOMEIN = "http://tartustour.ru";

    private static final String A_HREF_CSS_QUERY = "a[href]";

    private static Set<String> Garbage = new HashSet<String>();

    private static Set<String> UsedURL = new HashSet<String>();

    private static ArrayList<String> AllUrlQ = new ArrayList<String>();

    public static void main(String[] args) throws IOException
    {
        AllUrlQ.add(DOMEIN);

        breadthFirstSearch();

        AllUrlQ.removeAll(Garbage);

        Set<String> Result = new HashSet<String>(AllUrlQ);

        createSiteMap(Result);
    }

    private static void breadthFirstSearch() throws IOException
    {
        int delay = 1000 * Configuration.getInstance().getDelay();

        ListIterator<String> iter = AllUrlQ.listIterator();
        while (iter.hasNext())
        {
            String link = iter.next();
            if (!UsedURL.contains(link))
            {
                UsedURL.add(link);

                Document document = null;
                try
                {
                    document = Jsoup.connect(link).timeout(delay).get();
                }
                catch (HttpStatusException e)
                {
                    String message = String
                            .format("HttpStatusException for link: " + link);
                    logger.error(message);
                    Garbage.add(link);
                    continue;
                }
                catch (SocketTimeoutException e)
                {
                    String message = String
                            .format("SocketTimeoutException for link: " + link);
                    logger.error(message);
                    Garbage.add(link);
                    continue;
                }

                Elements links = document.select(A_HREF_CSS_QUERY);

                for (Element pLink : links)
                {
                    String linkHref = pLink.attr("href");
                    if (linkHref != null
                            && !AllUrlQ.contains(linkHref)
                            && !AllUrlQ.contains(DOMEIN + linkHref)
                            && linkHref.matches(Configuration.getInstance()
                                    .getPatterns().getPattern().get(0))
                            && !linkHref.contains("/image/")
                            && !linkHref.contains("/admin/")
                            && !linkHref.contains("/download/"))
                    {
                        if (linkHref.contains(DOMEIN))
                            iter.add(linkHref);
                        else
                            iter.add(DOMEIN + linkHref);
                        iter.previous();
                    }
                }
            }
        }
        return;
    }

    private static void createSiteMap(Set<String> Result)
            throws MalformedURLException
    {
        File baseDir = new File(Configuration.getInstance().getTempDirectory());
        WebSitemapGenerator wsg = new WebSitemapGenerator(DOMEIN, baseDir);

        for (String url : Result)
            wsg.addUrl(url);

        try
        {
            wsg.write();
            wsg.writeSitemapsWithIndex();
            System.out.println("CREATE SITEMAP.XML");
        }
        catch (RuntimeException e)
        {
            String message = String
                    .format("No URLs added, sitemap would be empty;"
                            + " you must add some URLs with addUrls.");
            logger.error(message);
        }
        return;
    }
}
