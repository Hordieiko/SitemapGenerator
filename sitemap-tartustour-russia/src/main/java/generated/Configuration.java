package generated;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

public class Configuration
{
    private static final Logger logger = Logger.getLogger(Configuration.class);

    private static final String CONFIGURATION_FILE_NAME = "configuration.xml";

    private static Configuration instance;

    private ConfigurationType configurationType;

    public static synchronized ConfigurationType getInstance()
    {
        if (instance == null)
        {
            instance = new Configuration();
        }
        return instance.getConfigurationTypeInstance();
    }

    private Configuration()
    {
    }

    public ConfigurationType getConfigurationTypeInstance()
    {
        if (configurationType == null)
        {
            loadConfigurationType();
        }
        return configurationType;
    }

    private void loadConfigurationType()
    {
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(CONFIGURATION_FILE_NAME);

            JAXBContext jaxbContext = JAXBContext
                    .newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            @SuppressWarnings("unchecked")
            JAXBElement<ConfigurationType> unmarshalledObject = (JAXBElement<ConfigurationType>) unmarshaller
                    .unmarshal(inputStream);

            configurationType = unmarshalledObject.getValue();
        }
        catch (JAXBException exception)
        {
            String message = String.format(
                    "The xml file %s could not be parsed.",
                    CONFIGURATION_FILE_NAME);
            logger.error(message);
        }
        catch (FileNotFoundException e)
        {
            String message = String.format("The file %s could not be found ",
                    CONFIGURATION_FILE_NAME);
            logger.error(message);
        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException exception)
                {
                    String message = String.format(
                            "The xml file %s could not be loaded properly.",
                            CONFIGURATION_FILE_NAME);
                    logger.error(message);
                }
            }
        }
    }
}