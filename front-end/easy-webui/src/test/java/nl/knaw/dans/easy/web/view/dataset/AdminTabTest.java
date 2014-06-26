package nl.knaw.dans.easy.web.view.dataset;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import nl.knaw.dans.common.lang.repo.DmoStoreId;
import nl.knaw.dans.easy.AuthzStrategyTestImpl;
import nl.knaw.dans.easy.EasyApplicationContextMock;
import nl.knaw.dans.easy.EasyUserTestImpl;
import nl.knaw.dans.easy.EasyWicketTester;
import nl.knaw.dans.easy.domain.dataset.DatasetImpl;
import nl.knaw.dans.easy.domain.model.PermissionSequence.State;
import nl.knaw.dans.easy.domain.model.user.EasyUser;
import nl.knaw.dans.easy.domain.model.user.EasyUser.Role;
import nl.knaw.dans.easy.domain.user.EasyUserImpl;
import nl.knaw.dans.easy.servicelayer.services.DatasetService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

public class AdminTabTest
{
    private static final String TAB_PATH = "tabs:tabs-container:tabs:3:link";
    private EasyApplicationContextMock applicationContext;
    private static DatasetImpl dataset;

    static public class PageWrapper extends DatasetViewPage
    {
        public PageWrapper()
        {
            super(dataset, DatasetViewPage.Mode.VIEW);
        }
    }

    public DatasetService mockDataset() throws Exception
    {
        final EasyUserImpl depositor = new EasyUserTestImpl("x:y");
        depositor.setInitials("D.E.");
        depositor.setSurname("Positor");
        dataset = new DatasetImpl("mocked-dataset:1");
        dataset.setState(State.Submitted.toString());
        dataset.setAuthzStrategy(new AuthzStrategyTestImpl());

        // needed twice because considered dirty
        dataset.getAdministrativeMetadata().setDepositor(depositor);
        dataset.getAdministrativeMetadata().setDepositor(depositor);

        // annotated SpringBean injection does not work for constructor of DatasetModel
        final DatasetService datasetService = PowerMock.createMock(DatasetService.class);
        expect(datasetService.getDataset(isA(EasyUser.class), isA(DmoStoreId.class))).andStubReturn(dataset);
        expect(datasetService.getAdditionalLicense(dataset)).andStubReturn(null);
        expect(datasetService.getLicenseVersions(dataset)).andStubReturn(null);
        expect(datasetService.getAdditionalLicenseVersions(dataset)).andStubReturn(null);
        return datasetService;
    }

    @Before
    public void mockApplicationContext() throws Exception
    {
        applicationContext = new EasyApplicationContextMock();
        applicationContext.expectStandardSecurity(false);
        applicationContext.expectDefaultResources();
        applicationContext.expectDisciplines();
        applicationContext.setDatasetService(mockDataset());
        applicationContext.expectNoJumpoff();
        applicationContext.expectNoItems();
    }

    @After
    public void reset()
    {
        PowerMock.resetAll();
    }

    @Test
    public void adminTabInVisible() throws Exception
    {
        applicationContext.expectAuthenticatedAsVisitor();
        final EasyWicketTester tester = startPage();
        tester.assertInvisible(TAB_PATH);
    }

    @Test
    public void adminTabVisible() throws Exception
    {
        applicationContext.expectAuthenticatedAsVisitor().addRole(Role.ARCHIVIST);
        final EasyWicketTester tester = startPage();
        tester.dumpPage();
        tester.assertVisible(TAB_PATH);
        tester.assertEnabled(TAB_PATH);
        tester.assertLabel(TAB_PATH + ":title", "Administration");
    }

    @Test
    public void adminTab() throws Exception
    {
        applicationContext.expectAuthenticatedAsVisitor().addRole(Role.ARCHIVIST);
        final EasyWicketTester tester = startPage();
        tester.clickLink(TAB_PATH);
        tester.dumpPage();// note that the IFrame for the upload is patched
        tester.debugComponentTrees();
    }

    private EasyWicketTester startPage()
    {
        final EasyWicketTester tester = EasyWicketTester.create(applicationContext);
        PowerMock.replayAll();
        tester.startPage(PageWrapper.class);
        return tester;
    }
}
