package ru.kos.neb_viewer;

/**
 *
 * @author kos
 */
import org.piccolo2d.event.PInputEvent;
import java.applet.*;


public class ProcessPaintFlashLinks extends
        Applet implements Runnable
{
    Thread m_ProcessPaintFlashLinks = null;
    PInputEvent e;
    private long timeout_position_edge=500;

    public ProcessPaintFlashLinks(PInputEvent e)
    {
        this.e=e;
    }
    public void run()
    {
        try { Thread.sleep(timeout_position_edge); } catch (InterruptedException ex) {}
        try
        {
            Utils.paintFlashLinks(e);
        }
        catch(java.lang.OutOfMemoryError ex) {}
        catch(java.lang.NullPointerException ex) {}
    }

    @Override
    public void start()
    {
        if (m_ProcessPaintFlashLinks == null)
        {
//            System.out.println("ProcessPaintFlashLinks started.");
            m_ProcessPaintFlashLinks = new Thread(this);
            m_ProcessPaintFlashLinks.start();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void stop()
    {
        if (m_ProcessPaintFlashLinks != null)
        {
//            System.out.println("ProcessPaintFlashLinks stoped.");
            m_ProcessPaintFlashLinks.stop();
            m_ProcessPaintFlashLinks = null;
        }
    }
}

class ProcessHideFlashLinks extends
        Applet implements Runnable
{
    Thread m_ProcessHideFlashLinks = null;
    public long timeout_flash_link = 30000;

    public ProcessHideFlashLinks()
    {
    }

    public ProcessHideFlashLinks(long timeout)
    {
        timeout_flash_link = timeout;
    }


    public void run()
    {
        try
        {
            try { Thread.sleep(timeout_flash_link); } catch(java.lang.InterruptedException e) {}
            Utils.StopAllFlashLinkProcesses();
            Utils.hideFlashLinks();
        }
        catch(java.lang.OutOfMemoryError ex) {}
        catch(java.lang.NullPointerException ex) {}
    }

    @Override
    public void start()
    {
        if (m_ProcessHideFlashLinks == null)
        {
//            System.out.println("Start process HideFlash.");
            m_ProcessHideFlashLinks = new Thread(this);
            m_ProcessHideFlashLinks.start();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void stop()
    {
        if (m_ProcessHideFlashLinks != null)
        {
//            System.out.println("Stop process HideFlash.");
            m_ProcessHideFlashLinks.stop();
            m_ProcessHideFlashLinks = null;
        }
    }


}

