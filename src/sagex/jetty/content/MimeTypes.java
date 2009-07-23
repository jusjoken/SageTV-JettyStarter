package sagex.jetty.content;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.io.Buffer;

public class MimeTypes extends org.mortbay.jetty.MimeTypes
{
    private static String MIME_FILE_LINE_DELIMITERS = "[\\t ]+"; // tab or space

    private boolean isSetup;
    private long fileLastReadTime;
    private String location;

    public MimeTypes()
    {
        setupMimeTypes();
    }
    
    private void setupMimeTypes()
    {
        if (isSetup)
        {
            return;
        }

        // check if called within 30 secs (to prevent to much FS access)
        if (fileLastReadTime + 30000 > System.currentTimeMillis())
        {
            return;
        }

        try
        {
            String fileLocation = getLocation();
            
            if (fileLocation == null)
            {
                return;
            }

            File mimeTypesFile = new File(fileLocation);

            // check if file has recently been loaded
            if (fileLastReadTime >= mimeTypesFile.lastModified())
            {
                fileLastReadTime = System.currentTimeMillis();
                return;
            }

            System.out.println("Loading mime types from file " + mimeTypesFile.getAbsolutePath());

            Map<String, String> mimeMap = new HashMap<String, String>();
            
            BufferedReader reader = new BufferedReader(new FileReader(mimeTypesFile));
            String line = null;
            
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#"))
                {
                    // comment
                    continue;
                }
                String[] mimeTypeLineParts = line.split(MIME_FILE_LINE_DELIMITERS);
                String mimeType = null;
                if (mimeTypeLineParts.length > 1)
                {
                    mimeType = mimeTypeLineParts[0];
                    for (int i = 1; i < mimeTypeLineParts.length; i++)
                    {
                        mimeMap.put(mimeTypeLineParts[i], mimeType);
                    }
                }
            }

            fileLastReadTime = System.currentTimeMillis();

            this.setMimeMap(mimeMap);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    @Override
    public Buffer getMimeByExtension(String arg0)
    {
        setupMimeTypes();
        return super.getMimeByExtension(arg0);
    }

    @Override
    public synchronized Map getMimeMap()
    {
        setupMimeTypes();
        return super.getMimeMap();
    }

    public String getLocation()
    {
        File sageHome = new File(System.getProperty("user.dir"));

        if (this.location != null)
        {
            // the context xml file or calling code specified a location
            File fileLocation = new File(location);
            if (!fileLocation.isAbsolute())
            {
                // the location is not absolute so make it relative to the sagetv home
                fileLocation = new File(sageHome, location);
                
                if (fileLocation.exists())
                {
                    return fileLocation.getAbsolutePath();
                }
                else
                {
                    System.out.println("mime.types file not found in override location " +
                             fileLocation.getAbsolutePath() + ".  Checking default location.");
                }
            }
        }

        // the default location
        File mimeTypesFile = new File(sageHome, "jetty/user/mime.types");

        if (!mimeTypesFile.exists())
        {
            // the backward-compatible location for nielm's webserver
            File defaultMimeTypesFile = mimeTypesFile;
            mimeTypesFile = new File(sageHome, "webserver/mime.types");

            if (!mimeTypesFile.exists())
            {
                System.out.println("mime.types file not found in default location " +
                        defaultMimeTypesFile.getAbsolutePath() + " or legacy location " +
                        mimeTypesFile.getAbsolutePath() + ".  Jetty's default mime types will " +
                        "be the only types available.");
                return null;
            }
        }
        
        return mimeTypesFile.getAbsolutePath();
    }
    
    public void setLocation(String location)
    {
        this.location = location;
    }
    
    public static void main(String[] args)
    {
        // nielm format
        String[] a = "video/mp4    \t      mp4".split("[\\t ]+"); 
        String[] b = "video/mp2t              TS".split("[\\t ]+"); 
        String[] c = "video/mp4v-es".split("[\\t ]+"); 
        String[] d = "video/mp4 \t \t \t        mp4".split("[\\t ]+"); 
        String[] e = "video/mpeg          mpeg mpg mpe m2v m4v".split("[\\t ]+"); 

        System.out.println("a: " + a.length);
        System.out.println("b: " + b.length);
        System.out.println("c: " + c.length);
        System.out.println("d: " + d.length);
        System.out.println("e: " + e.length);
    }
}
