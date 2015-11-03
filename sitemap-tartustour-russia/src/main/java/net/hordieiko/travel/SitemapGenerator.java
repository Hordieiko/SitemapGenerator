package net.hordieiko.travel;

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

public class SitemapGenerator
{
    private static final String URL = "http://tartustour.ru";

    private static final String A_HREF_CSS_QUERY = "a[href]";

    private static Set<String> Garbage = new HashSet<String>();

    private static Set<String> UsedURL = new HashSet<String>();

    private static ArrayList<String> AllUrlQ = new ArrayList<String>();

    public static void main(String[] args) throws IOException
    {
        AllUrlQ.add("http://tartustour.ru/aktsii/");

        breadthFirstSearch();

        AllUrlQ.removeAll(Garbage);

        Set<String> Result = new HashSet<String>(AllUrlQ);

        createSiteMap(Result);
    }

    private static void breadthFirstSearch() throws IOException
    {
        ListIterator<String> iter = AllUrlQ.listIterator();
        while (iter.hasNext())
        {
            String link = iter.next();

            System.out.println("iter.next(): " + link);
            if (!UsedURL.contains(link))
            {
                Document document = null;
                try
                {
                    document = Jsoup.connect(link).timeout(90000).get();
                }
                catch (HttpStatusException e)
                {
                    System.out.println("HttpStatusException");
                    Garbage.add(link);
                    continue;
                }
                catch (SocketTimeoutException e)
                {
                    System.out.println("SocketTimeoutException");
                    Garbage.add(link);
                    continue;
                }

                UsedURL.add(link);

                Elements links = document.select(A_HREF_CSS_QUERY);

                for (Element pLink : links)
                {
                    String linkHref = pLink.attr("href");
                    // |(^/.*)
                    if (linkHref != null && !UsedURL.contains(linkHref)
                            && !AllUrlQ.contains(linkHref)
                            && !AllUrlQ.contains(URL + linkHref)
                            && linkHref.matches("(^" + URL + "/.*)|(^/.*)")
                            && !linkHref.contains("/image/")
                            && !linkHref.contains("/admin/")
                            && !linkHref.contains("/download/"))
                    {
                        if (linkHref.contains(URL))
                            iter.add(linkHref);
                        else
                            iter.add(URL + linkHref);
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
        WebSitemapGenerator wsg = new WebSitemapGenerator(URL, new File("D:\\"));
        
        for (String url : Result)
            wsg.addUrl(url);

        wsg.write();
        wsg.writeSitemapsWithIndex(); // generate the sitemap_index.xml
        System.out.println("CREATE SITEMAP.XML");
    }
}
