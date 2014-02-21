package nl.knaw.dans.easy.web.view.dataset;

import static org.easymock.EasyMock.isA;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.easy.domain.download.DownloadList;
import nl.knaw.dans.easy.domain.model.Dataset;
import nl.knaw.dans.easy.domain.model.user.EasyUser;
import nl.knaw.dans.easy.domain.model.user.EasyUser.Role;
import nl.knaw.dans.easy.domain.user.EasyUserAnonymous;
import nl.knaw.dans.easy.domain.user.EasyUserImpl;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.tester.ITestPanelSource;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

public class DownloadActivityLogPanelTest extends ActivityLogFixture implements Serializable
{
    private static final EasyUserImpl ARCHIVIST = new EasyUserImpl(Role.ARCHIVIST);
    private static final EasyUserImpl USER = new EasyUserImpl(Role.USER);

    private static final long serialVersionUID = 1L;

    private static final String ANONYMOUS_DOWNLOAD_LINE = "2013-12-13T00:00:00.000+01:00;anonymous; ; ; ; ;null;\n";
    private static final String PANEL = "panel";
    private static final String PANEL_DOWNLOAD_CSV = PANEL + ":" + DownloadActivityLogPanel.DOWNLOAD_CSV;

    @Test
    public void noDLH() throws Exception
    {
        expectInvisible(null, ARCHIVIST);
    }

    @Test
    public void emptyDLH() throws Exception
    {
        expectInvisible(createDownloadList(), ARCHIVIST);
    }

    @Test
    public void byUser() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, null, DOWNLOAD_DATE_TIME);
        expectInvisible(downloadList, new EasyUserImpl(Role.USER));
    }

    @Test
    public void byAdmin() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, null, DOWNLOAD_DATE_TIME);
        expectInvisible(downloadList, new EasyUserImpl(Role.ADMIN));
    }

    @Test
    public void withoutUser() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, null, DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, ANONYMOUS_DOWNLOAD_LINE);
    }

    @Test
    public void withAnonymous() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, EasyUserAnonymous.getInstance(), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, ANONYMOUS_DOWNLOAD_LINE);
    }

    @Test
    public void userWantsNoActionLog() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, mockUser(false), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, "2013-12-13T00:00:00.000+01:00;userid;surname;email;organization;function;null;\n");
    }

    @Test
    public void withKnownUser() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, mockUser(true), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, "2013-12-13T00:00:00.000+01:00;userid;surname;email;organization;function;null;\n");
    }

    @Test
    public void withNotFoundUser() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, mockNotFoundUser(), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, ANONYMOUS_DOWNLOAD_LINE);
    }

    @Test
    public void withEmptyUserValues() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, mockUserWithEmptyValues(), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, "2013-12-13T00:00:00.000+01:00;userid;;null;null;null;null;\n");
    }

    @Test
    public void withEmptyDownloaderID() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, new EasyUserImpl(""), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, ANONYMOUS_DOWNLOAD_LINE);
    }

    @Test
    public void withNotFoundUserService() throws Exception
    {
        final DownloadList downloadList = createDownloadList();
        downloadList.addDownload(FILE_ITEM_VO, mockNotFoundUserService(), DOWNLOAD_DATE_TIME);
        expect(downloadList, ARCHIVIST, ANONYMOUS_DOWNLOAD_LINE);
    }

    @Test
    public void withNotFoundDatasetService() throws Exception
    {
        EasyMock.expect(datasetService.getDownloadHistoryFor(isA(EasyUser.class), isA(Dataset.class), isA(DateTime.class))).andStubThrow(
                new ServiceException(""));
        expectInvisible(null, new EasyUserImpl(Role.USER));
    }

    @Test
    public void archivistFeb2013issue560() throws Exception
    {
        expect(new MockedDLHL36028(userService, ARCHIVIST).getList(), ARCHIVIST, MockedDLHL36028.getArchivistExpectation());
    }

    @Test
    public void datasetOwnerFeb2013issue560() throws Exception
    {
        WicketTester tester = run(new MockedDLHL36028(userService, USER).getList(), USER);
        tester.assertInvisible(PANEL);
        tester.assertInvisible(PANEL_DOWNLOAD_CSV);
        tester.assertEnabled(PANEL_DOWNLOAD_CSV);
        // code smell: invisible but enabled
    }

    private void expectInvisible(final DownloadList downloadList, final EasyUserImpl easyUser) throws Exception
    {
        final WicketTester tester = run(downloadList, easyUser);
        tester.assertInvisible(PANEL);
        tester.assertInvisible(PANEL_DOWNLOAD_CSV);
    }

    private void expect(final DownloadList downloadList, EasyUser sessionUser, final String lines) throws Exception
    {
        final WicketTester tester = run(downloadList, sessionUser);
        tester.assertVisible(PANEL);
        tester.assertVisible(PANEL_DOWNLOAD_CSV);
        tester.assertEnabled(PANEL_DOWNLOAD_CSV);
        tester.clickLink(PANEL_DOWNLOAD_CSV);
        String download = tester.getServletResponse().getDocument();
        String downloadWithoutHeaderLine = download.replaceFirst("^[^\\n]*\\n", "");
        assertThat(downloadWithoutHeaderLine, is(lines));
    }

    private WicketTester run(final DownloadList downloadList, final EasyUser sessionUser) throws Exception
    {
        // the boolean arguments are for dataset owners viewing the web page, hence not important here
        final Dataset mockedDataset = mockDataset(downloadList, sessionUser, false, false, true);
        final Session mockedSession = mockSessionFor_Component_isActionAuthourized();
        PowerMock.replayAll();

        final WicketTester tester = createWicketTester();
        tester.startPanel(new ITestPanelSource()
        {
            private static final long serialVersionUID = 1L;

            public Panel getTestPanel(final String panelId)
            {
                return new DownloadActivityLogPanel(panelId, mockedDataset, sessionUser)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Session getSession()
                    {
                        return mockedSession;
                    }
                };
            }
        });
        return tester;
    }
}