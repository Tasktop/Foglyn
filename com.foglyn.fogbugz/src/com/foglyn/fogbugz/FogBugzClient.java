package com.foglyn.fogbugz;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import nu.xom.Document;
import nu.xom.Element;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;

import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzData.FogBugzDataBuilder;
import com.foglyn.fogbugz.FogBugzEvent.EventID;
import com.foglyn.fogbugz.FogBugzFilter.FilterID;
import com.foglyn.fogbugz.FogBugzFilter.FilterType;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

/**
 * This class uses FogBugz API to obtain information from remote FogBugz server.
 */
public class FogBugzClient {
    private static final String CASE_COLUMNS_NO_EVENTS = "ixBug,ixBugEventLatest,sTitle,fOpen,dtOpened," +
    		"dtClosed,dtResolved,ixProject,ixPersonAssignedTo,ixPersonOpenedBy,ixPersonResolvedBy," +
    		"ixArea,ixStatus,ixCategory,ixPriority,ixFixFor,dtLastUpdated," +
    		"hrsOrigEst,hrsCurrEst,hrsElapsed,dtDue,ixRelatedBugs";
    
    private static final String EVENTS_COLUMN = ",events";
    
    private static final String FOGBUGZ7_COLUMNS = ",ixBugParent,ixBugChildren,tags";
    
    // Version of API this class uses. If remote FogBugz server returns minVersion higher than this, we are out of luck. 
    private static final int MIN_VERSION = 5;

    private final Request request;

    /**
     * Version of remote API.
     */
    private final int apiVersion;
    
    // Base Fogbugz API URL, usually ends with "api.asp?" or "api.php?". Suffix is returned from /api.xml call.
    private final String fogbugzBaseApiURL;

    private final Object tokenLock;
    
    // Guarded by tokenLock
    private String token; // token obtained after logon
    
    // Guarded by tokenLock
    /** FogBugz API URL with appended token and ampersand (https://.../api.asp?token=23752947dbakjsgdf&) */
    private String apiURL;
    
    private final AtomicReference<FogBugzData> data;
    
    private final Log log;
    
    public static FogBugzClient createFogBugzClient(AbstractWebLocation repositoryLocation, HttpClient httpClient, IProgressMonitor monitor) throws FogBugzException {
        Assert.isNotNull(repositoryLocation);
        Assert.isNotNull(httpClient);
        Assert.isNotNull(monitor);
        
        monitor.subTask("Contacting FogBugz server");
        
        String baseURL = getBaseURL(repositoryLocation);

        Request request = new Request(httpClient, repositoryLocation);
        Document doc = request.requestAPI(baseURL, monitor);

        monitor.worked(1);
        
        int apiVersion = Integer.parseInt(XOMUtils.xpathValueOf(doc, "/response/version"));
        int minVersion = Integer.parseInt(XOMUtils.xpathValueOf(doc, "/response/minversion"));
        
        if (apiVersion < MIN_VERSION || minVersion > MIN_VERSION) {
            throw new FogBugzException(
                    "Remote server doesn't support required version of API. Server is at version " + apiVersion +
                    ", minimum supported version is " + minVersion + ". Client implements version " + MIN_VERSION);
        }
        
        String apiUrl = URI.create(baseURL).resolve(XOMUtils.xpathValueOf(doc, "/response/url")).toASCIIString();

        return new FogBugzClient(repositoryLocation, httpClient, apiUrl, apiVersion);
    }
    
    private static String getBaseURL(AbstractWebLocation repositoryLocation) {
        String base = repositoryLocation.getUrl();

        if (!base.endsWith("/")) {
            base = base + "/";
        }
        base = base + "api.xml";
        return base;
    }
    
    /**
     * 
     * @param repositoryLocation
     * @param httpClient
     * @param fogbugzApiURL
     *            URL where FogBugz API is accessible, e.g.
     *            <code>https://.../api.asp?</code>. This must end with ? or
     *            &amp; because more query parameters are appended to this
     *            string when performing commands.
     * @param apiVersion version of remote API
     * @param minVersion minimum compatible version of API
     */
    FogBugzClient(AbstractWebLocation repositoryLocation, HttpClient httpClient, String fogbugzApiURL, int apiVersion) {
        this.log = Logging.getLogger("client.url" + "@" + repositoryLocation.getUrl());
        
        this.tokenLock = new Object();

        this.request = new Request(httpClient, repositoryLocation);
        this.fogbugzBaseApiURL = fogbugzApiURL;
        this.apiVersion = apiVersion;
        
        setToken(null);
        
        this.data = new AtomicReference<FogBugzData>();
        this.data.set(new FogBugzData.FogBugzDataBuilder().build());
    }
    
    private void setToken(String token) {
        synchronized (tokenLock) {
            if (token == null) {
                this.token = null;
                this.apiURL = fogbugzBaseApiURL;
            } else {
                this.token = token;
                this.apiURL = fogbugzBaseApiURL + "token=" + Utils.urlEncode(token) + "&";
            }
        }
    }
    
    private String getToken() {
        synchronized (tokenLock) {
            return this.token;
        }
    }

    private String getApiURL() {
        synchronized (tokenLock) {
            return apiURL;
        }
    }
    
    private String getPostURL() {
        // remove trailing ? from URL
        return fogbugzBaseApiURL.substring(0, fogbugzBaseApiURL.length() - 1);
    }
    
    private String getCaseURL(CaseID caseID) {
        String r = Utils.replaceSuffix(fogbugzBaseApiURL, "api.asp?", "default.asp?", "api.php?", "default.php?");
        if (r != fogbugzBaseApiURL) {
            return r + caseID;
        }
        
        return null;
    }

    /**
     * Generates full URL of command, incl. token and all parameters. This can
     * be used with {@link Request#requestAPI(String, IProgressMonitor)}, or
     * other methods from {@link Request} class.
     * 
     * @param cmd
     * @param params
     * @return
     */
    private String command(String cmd, String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(getApiURL());
        sb.append(Utils.urlEncode("cmd"));
        sb.append("=");
        sb.append(Utils.urlEncode(cmd));
        
        for (int i = 0; i < (params.length / 2); i++) {
            String paramName = params[2*i];
            String paramValue = params[2*i + 1];

            Assert.isNotNull(paramName, "paramenter name " + i);
            Assert.isNotNull(paramName, "paramenter value " + i);
            
            sb.append("&");
            sb.append(Utils.urlEncode(paramName));
            sb.append("=");
            sb.append(Utils.urlEncode(paramValue));
        }
        
        return sb.toString();
    }
    
    /**
     * Performs login into the server. When login succeeds, login token is
     * stored and used for subsequent uses of this client, until
     * {@link #logout(IProgressMonitor)} method is called.
     * 
     * @throws FogBugzResponseIncorrectPasswordOrUsername if username or password is incorrect (returned by FogBugz server)
     */
    public void login(String username, String password, IProgressMonitor monitor) throws FogBugzException {
        Assert.isNotNull(username, "username");
        Assert.isNotNull(password, "password");
        
        monitor.subTask("Logging into server");
        
        // Use POST for logging in. FogBugz enforces HTTPS for POST requests, and this will check that
        // supplied URL is correct.
        // Furthermore, POST request doesn't show username/password in Apache HTTP Client Log.
        
        List<Part> parts = new ArrayList<Part>();
        parts.add(stringPart("cmd", "logon"));
        parts.add(stringPart("email", username));
        parts.add(stringPart("password", password));
        
        String postURL = getPostURL();
        Document logon = null;
        try {
            logon = request.post(postURL, parts, monitor);
        } catch (FogBugzHttpException e) {
            checkUseDifferentRepositoryProblem(e, postURL);
            
            throw e;
        }
        
        monitor.worked(1);
        
        String token = XOMUtils.xpathValueOf(logon, "/response/token");
        
        setToken(token);
    }

    /**
     * Will throw exception if FogBugz wants to use different repository
     */
    private void checkUseDifferentRepositoryProblem(FogBugzHttpException e, String usedURL) throws FogBugzException {
        if (e.getHttpCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            return;
        }
        
        String location = e.getResponseHeaders().get("location");
        if (location == null) {
            return;
        }

        String orig = usedURL.toLowerCase();
        String received = location.toLowerCase();

        String locationForUser = Utils.replaceSuffix(location, "api.asp", "", "api.php", "");
        
        // if requested location is same as supplied, but with HTTPS insteada of HTTP, we send simplified error to user
        if (orig.startsWith("http:") && received.startsWith("https:") && 
                orig.substring("http:".length()).equals(received.substring("https:".length()))) {
            throw new FogBugzException("This server requires HTTPS protocol (i.e., " + locationForUser + ")");
        }
        
        throw new FogBugzException("Please use '" + locationForUser + "' as server repository");
    }

    public void logout(IProgressMonitor monitor) throws FogBugzException {
        monitor.subTask("Logging off");
        
        try {
            request.requestAPI(command("logoff"), monitor);
        } catch (FogBugzResponseNogLoggedOnException e) {
            // ok, expected. FogBugz replies with this error on logoff.
        }
        
        monitor.worked(1);
        
        setToken(null);
    }
    
    public List<FogBugzFilter> listFilters(IProgressMonitor monitor) throws FogBugzException {
        monitor.subTask("Getting list of filters");
        
        Document response = request.requestAPI(command("listFilters"), monitor);
        
        monitor.worked(1);
        
        List<Element> filters = XOMUtils.xpathElements(response, "/response/filters/filter");
        
        List<FogBugzFilter> result = new ArrayList<FogBugzFilter>();
        
        for (Element e: filters) {
            String type = e.getAttributeValue("type");
            String id = e.getAttributeValue("sFilter");
            String desc = e.getValue();
            
            FilterType filterType = FilterType.UNKNOWN;
            if ("builtin".equals(type)) {
                filterType = FilterType.BUILTIN;
            } else if ("saved".equals(type)) {
                filterType = FilterType.SAVED;
            } else if ("shared".equals(type)) {
                filterType = FilterType.SHARED;
            }
            
            String status = e.getAttributeValue("status");
            
            FogBugzFilter filter = new FogBugzFilter(filterType, FilterID.valueOf(id), desc, "current".equals(status));
            result.add(filter);
        }
        
        return result;
    }

//    private FogBugzFilter getCurrentFilter(IProgressMonitor monitor) throws CoreException {
//        List<FogBugzFilter> filters = listFilters(monitor);
//        
//        for (FogBugzFilter f: filters) {
//            if (f.isCurrent()) {
//                return f;
//            }
//        }
//        
//        // FIXME: builtin filter is never returned as 'current'
//        return null;
//    }
    
    private void setCurrentFilter(FilterID filterID, IProgressMonitor monitor) throws FogBugzException {
        Document response = request.requestAPI(command("saveFilter", "sFilter", filterID.toString()), monitor);

        Assert.isNotNull(response);
        
        // no exception indicates success
    }
    
    public List<FogBugzCase> listTasksForFilter(FilterID filterID, IProgressMonitor monitor, boolean loadEvents) throws FogBugzException {
        Assert.isNotNull(filterID, "filterID");

        monitor.subTask("Fetching cases from filter");
        
//        FogBugzFilter currentFilter = getCurrentFilter(monitor);
        
        setCurrentFilter(filterID, monitor);
        
        monitor.worked(1);
        
        List<FogBugzCase> result = fetchList(request, command("search", "cols", getColumns(loadEvents)), "/response/cases/case", monitor, new CaseMapper(monitor, loadEvents));

        monitor.worked(1);
        
//        if (currentFilter != null) {
//            try {
//                setCurrentFilter(currentFilter.getFilterID(), monitor);
//            } catch (CoreException e) {
//                // can't set filter back ... ignore :-(
//            }
//        }
        
        return result;
    }

    private String getColumns(boolean loadEvents) {
        StringBuilder cols = new StringBuilder(CASE_COLUMNS_NO_EVENTS.length() + FOGBUGZ7_COLUMNS.length() + EVENTS_COLUMN.length());
        
        cols.append(CASE_COLUMNS_NO_EVENTS);
        
        if (isFogBugz7Repository()) {
            cols.append(FOGBUGZ7_COLUMNS);
        }
        
        if (loadEvents) {
            cols.append(EVENTS_COLUMN);
        }
        
        return cols.toString();
    }

    private FogBugzCase createCase(Element e, IProgressMonitor monitor, boolean loadEvents) throws FogBugzException {
        FogBugzCase fbCase = new FogBugzCase();
        
        if ("".equals(XOMUtils.xpathValueOf(e, "ixBug").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixBugEventLatest").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixProject").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixPersonAssignedTo").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixPersonOpenedBy").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixPersonResolvedBy").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixArea").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixStatus").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixCategory").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixPriority").trim()) ||
                "".equals(XOMUtils.xpathValueOf(e, "ixFixFor").trim())) {
            
            log.debug("Some ix element is empty or missing, full case element: " + e.toXML());
        }
                
        CaseID caseID = CaseID.valueOf(XOMUtils.xpathValueOf(e, "ixBug"));
        fbCase.setCaseID(caseID);
        
        fbCase.setLatestEvent(EventID.valueOf(XOMUtils.xpathValueOf(e, "ixBugEventLatest")));
        fbCase.setTitle(XOMUtils.xpathValueOf(e, "sTitle"));
        fbCase.setTaskURL(getCaseURL(fbCase.getCaseID()));
        fbCase.setOpen(Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fOpen")));
        fbCase.setProject(ProjectID.valueOf(XOMUtils.xpathValueOf(e, "ixProject")));
        fbCase.setOpenedDate(Parsers.parseDate(XOMUtils.xpathValueOf(e, "dtOpened")));
        fbCase.setResolvedDate(Parsers.parseDate(XOMUtils.xpathValueOf(e, "dtResolved")));
        fbCase.setClosedDate(Parsers.parseDate(XOMUtils.xpathValueOf(e, "dtClosed")));
        fbCase.setAssignedTo(PersonID.valueOf(XOMUtils.xpathValueOf(e, "ixPersonAssignedTo")));
        fbCase.setOpenedBy(PersonID.valueOf(XOMUtils.xpathValueOf(e, "ixPersonOpenedBy")));
        
        String resolvedBy = XOMUtils.xpathValueOf(e, "ixPersonResolvedBy");
        if (resolvedBy.trim().length() == 0) {
            fbCase.setResolvedBy(null);
        } else {
            fbCase.setResolvedBy(PersonID.valueOf(resolvedBy));
        }
        
        fbCase.setArea(AreaID.valueOf(XOMUtils.xpathValueOf(e, "ixArea")));
        fbCase.setStatus(StatusID.valueOf(XOMUtils.xpathValueOf(e, "ixStatus")));
        fbCase.setCategory(CategoryID.valueOf(XOMUtils.xpathValueOf(e, "ixCategory")));
        fbCase.setPriority(PriorityID.valueOf(XOMUtils.xpathValueOf(e, "ixPriority")));
        fbCase.setFixFor(FixForID.valueOf(XOMUtils.xpathValueOf(e, "ixFixFor")));
        fbCase.setDueDate(Parsers.parseDate(XOMUtils.xpathValueOf(e, "dtDue")));
        fbCase.setRelatedBugs(Parsers.parseBugList(XOMUtils.xpathValueOf(e, "ixRelatedBugs")));
        fbCase.setChildrenCases(Parsers.parseBugList(XOMUtils.xpathValueOf(e, "ixBugChildren")));
        fbCase.setParentCase(Parsers.parseCaseID(XOMUtils.xpathValueOf(e, "ixBugParent")));
        fbCase.setTags(Parsers.parseTags(XOMUtils.xpathElement(e, "tags")));

        fbCase.setOriginalEstimateInHours(Parsers.parseHours(XOMUtils.xpathValueOf(e, "hrsOrigEst"), false));
        
        boolean hasEstimate = fbCase.getOriginalEstimateInHours() != null;
        
        fbCase.setCurrentEstimateInHours(Parsers.parseHours(XOMUtils.xpathValueOf(e, "hrsCurrEst"), hasEstimate));
        fbCase.setElapsedTimeInHours(Parsers.parseHours(XOMUtils.xpathValueOf(e, "hrsElapsed"), hasEstimate));
        
        fbCase.setRemainingTimeInHours(computeRemainingTime(fbCase.getCurrentEstimateInHours(), fbCase.getElapsedTimeInHours()));
        
        fbCase.setConvertedOriginalEstimate(convertToDaysHoursMinutes(fbCase.getOriginalEstimateInHours()));
        fbCase.setConvertedCurrentEstimate(convertToDaysHoursMinutes(fbCase.getCurrentEstimateInHours()));
        fbCase.setConvertedElapsedTime(convertToDaysHoursMinutes(fbCase.getElapsedTimeInHours()));
        fbCase.setConvertedRemainingTime(convertToDaysHoursMinutes(fbCase.getRemainingTimeInHours()));
        
        fbCase.setActions(Parsers.parseActions(XOMUtils.xpathValueOf(e, "@operations")));
        
        // FIXME: set closed by?
        
        fbCase.setLastUpdated(Parsers.parseDate(XOMUtils.xpathValueOf(e, "dtLastUpdated")));
        
        if (loadEvents) {
            monitor.subTask("Fetching details about case " + caseID.toString());
    
            addEvents(e, fbCase, monitor);
            
            monitor.worked(1);
        }
        
        return fbCase;
    }

    /**
     * Computes remaining time from current estimate and elapsed time.
     * @param current current estimate in hours, can be null
     * @param elapsed elapsed time in hours, can be null
     */
    private BigDecimal computeRemainingTime(BigDecimal current, BigDecimal elapsed) {
        BigDecimal remaining = current; // possibly null
        
        if (current != null && elapsed != null) {
            remaining = current.subtract(elapsed);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
        }
        return remaining;
    }

    /**
     * @param hours
     * @return Converts given time in hours to (days/hours/minutes) 
     */
    private DaysHoursMinutes convertToDaysHoursMinutes(BigDecimal hours) {
        if (hours == null) {
            return null;
        }
        
        WorkingSchedule ws = getSiteWorkingSchedule();
        if (ws == null) {
            return null;
        }
        
        BigDecimal workingHours = ws.getWorkingHoursPerDay();
        
        return DaysHoursMinutes.convertToDaysHoursMinutes(hours, workingHours);
    }

    private void addEvents(Element caseElement, FogBugzCase fbCase, IProgressMonitor monitor) throws FogBugzException {
        Element eventsElement = XOMUtils.xpathElement(caseElement, "events");
        if (eventsElement != null) {
            List<Element> events = XOMUtils.xpathElements(caseElement, "events/event");
            
            List<FogBugzEvent> caseEvents = new ArrayList<FogBugzEvent>();
            for (Element ee: events) {
                FogBugzEvent ev = new FogBugzEvent();
                
                ev.setEventID(EventID.valueOf(XOMUtils.xpathValueOf(ee, "ixBugEvent")));
                ev.setVerb(Utils.transformNumericAndCommonEntities(XOMUtils.xpathValueOf(ee, "sVerb")));
                ev.setEventDescription(Utils.transformNumericAndCommonEntities(XOMUtils.xpathValueOf(ee, "evtDescription")));
                ev.setInitiator(PersonID.valueOf(XOMUtils.xpathValueOf(ee, "ixPerson")));
                ev.setDate(Parsers.parseDate(XOMUtils.xpathValueOf(ee, "dt")));
                ev.setText(XOMUtils.xpathValueOf(ee, "s"));
                ev.setChanges(XOMUtils.xpathValueOf(ee, "sChanges"));
                
                String bEmail = XOMUtils.xpathValueOf(ee, "bEmail");
                if ("".equals(bEmail)) {
                    bEmail = XOMUtils.xpathValueOf(ee, "fEmail"); // FogBugz 7
                }
                
                ev.setEmail(Boolean.parseBoolean(bEmail));
                if (ev.isEmail()) {
                    ev.setEmailFrom(XOMUtils.xpathValueOf(ee, "sFrom"));
                }
                
                List<Element> attachmentElements = XOMUtils.xpathElements(ee, "rgAttachments/attachment");
                List<FogBugzAttachment> attachments = new ArrayList<FogBugzAttachment>();
                
                for (Element ae: attachmentElements) {
                    String filename = XOMUtils.xpathValueOf(ae, "sFileName");
                    String url = XOMUtils.xpathValueOf(ae, "sURL");
                    
                    url = Utils.transformCommonEntities(url);
                    
                    FogBugzAttachment attach = new FogBugzAttachment(filename, url);
                    
                    fetchAdditionalAttachmentDetails(attach, monitor);
                    
                    attachments.add(attach);
                }
                
                if (!attachments.isEmpty()) {
                    ev.setAttachments(attachments);
                }
                
                caseEvents.add(ev);
            }
            
            fbCase.setEvents(caseEvents);
        }
    }

    private void fetchAdditionalAttachmentDetails(FogBugzAttachment attach, IProgressMonitor monitor) throws FogBugzException {
        attach.setUrlWithHost(getAttachmentUrlWithHost(attach.getUrlComponent()));
        
        String urlWithHostAndToken = getFullAttachmentUrlWithHostAndToken(attach.getUrlComponent());
        
        Map<String, String> headers = request.getHeaders(urlWithHostAndToken, monitor);
        
        // keys are in lower-case
        if (headers.containsKey("content-length")) {
            try {
                long length = Long.parseLong(headers.get("content-length"));
                if (length > 0) {
                    attach.setLength(length);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        attach.setMimetype(headers.get("content-type"));
    }

    private String getAttachmentUrlWithHost(String urlComponent) {
        return URI.create(this.fogbugzBaseApiURL).resolve(urlComponent).toASCIIString();
    }

    private String getFullAttachmentUrlWithHostAndToken(String urlComponent) {
        String urlWithHost = getAttachmentUrlWithHost(urlComponent);
        
        String urlWithHostAndToken = urlWithHost + "&token=" + Utils.urlEncode(getToken());
        return urlWithHostAndToken;
    }

    public FogBugzCase getCase(String taskID, IProgressMonitor monitor) throws FogBugzException {
        Assert.isNotNull(taskID, "taskID");
        
        Document response = request.requestAPI(command("search", "q", taskID, "cols", getColumns(true)), monitor);
        
        List<Element> casesElements = XOMUtils.xpathElements(response, "/response/cases/case");
        if (casesElements.isEmpty()) {
            return null;
        }
        
        return createCase(casesElements.get(0), monitor, true);
    }

    public List<FogBugzCase> search(String searchString, IProgressMonitor monitor, boolean loadEvents) throws FogBugzException {
        log.debug("Searching: " + searchString + ", loadEvents: " + loadEvents);
        
        monitor.subTask("Searching for cases");
        
        List<FogBugzCase> result = fetchList(
                request,
                command("search", 
                        "q", searchString, 
                        "cols", getColumns(loadEvents)), 
                        "/response/cases/case", monitor, new CaseMapper(monitor, loadEvents));
        
        monitor.worked(1);
        
        log.debug("Found " + result.size() + " cases");
        
        return result;
    }
    
    private <T> List<T> fetch(IProgressMonitor monitor, String listCommand, String xpath, Mapper<T> mapper, String... parameters) throws FogBugzException {
        return fetchList(request, command(listCommand, parameters), xpath, monitor, mapper);
    }
    
    private <T> List<T> fetchList(Request request, String url, String xpath, IProgressMonitor monitor, Mapper<T> mapper) throws FogBugzException {
        log.debug("Fetching from " + url);
        
        Document response = request.requestAPI(url, monitor);

        log.debug("Parsing response");
        List<Element> elements = XOMUtils.xpathElements(response, xpath);
        
        List<T> result = new ArrayList<T>();
        
        for (Element e: elements) {
            result.add(mapper.mapElement(e));
        }
        
        log.debug("Fetched " + result.size() + " objects");
        
        return result;
    }

    private <T> T fetchSingle(Request request, String url, String xpath, IProgressMonitor monitor, Mapper<T> mapper) throws FogBugzException {
        log.debug("Fetching single from " + url);
        
        Document response = request.requestAPI(url, monitor);
        
        log.debug("Parsing response");
        Element element = XOMUtils.xpathElement(response, xpath);
        if (element == null) {
            log.debug("No object");
            return null;
        }
        
        T result = mapper.mapElement(element);
        
        log.debug("Fetched single object");
        return result;
    }
    
    public FogBugzCategory getCategory(CategoryID categoryID) {
        return data.get().getCategory(categoryID);
    }
    
    public Collection<FogBugzCategory> getAllCategories() {
        return data.get().getAllCategories();
    }
    
    public FogBugzPriority getPriority(PriorityID priorityID) {
        return data.get().getPriority(priorityID);
    }

    public Collection<FogBugzPriority> getAllPriorities() {
        return data.get().getAllPriorities();
    }
    
    public FogBugzStatus getStatus(StatusID statusID) {
        return data.get().getStatus(statusID);
    }

    public Collection<FogBugzStatus> getAllStatuses() {
        return data.get().getAllStatuses();
    }

    public FogBugzPerson getPerson(PersonID personID) {
        FogBugzData fbData = data.get();
        FogBugzPerson result = fbData.getPerson(personID);
        
        if (result != null) {
            return result;
        }
        
        if (fbData.isNonExistant(personID)) {
            return null;
        }

        FogBugzPerson p = null;
        try {
            p = fetchSingle(request, command("viewPerson", "ixPerson", personID.toString()), "/response/person", new NullProgressMonitor(), new PersonMapper());
        } catch (FogBugzException e) {
            // ignore this problem ... even some legitimate problems are ignored
            // here, in assumption that they will arise again, if they persist
            return null;
        }

        FogBugzDataBuilder b = new FogBugzDataBuilder(fbData);
        if (p == null) {
            b.addNonExistantPersonID(personID);
        } else {
            if (p.isInactive()) {
                b.addInactivePerson(p);
            } else {
                // new person?
                b.addPerson(p);
            }
        }
        
        data.set(b.build());
        
        return p;
    }

    /**
     * @return collection of active people (normal or virtual). Inactive people
     *         are not returned, even if they were already fetched from server.
     */
    public Collection<FogBugzPerson> getAllPeople() {
        return data.get().getAllPeople();
    }

    public FogBugzProject getProject(ProjectID projectID) {
        return data.get().getProject(projectID);
    }
    
    public Collection<FogBugzProject> getAllProjects() {
        return data.get().getAllProjects();
    }
    
    public FogBugzArea getArea(AreaID areaID) {
        return data.get().getArea(areaID);
    }
    
    public Collection<FogBugzArea> getAllAreas() {
        return data.get().getAllAreas();
    }

    public FogBugzFixFor getFixFor(FixForID fixFor) {
        return data.get().getFixFor(fixFor);
    }
    
    public Collection<FogBugzFixFor> getAllFixFors() {
        return data.get().getAllFixFors();
    }

    /**
     * @return working schedule for logged-in user (may be null, if caches has not yet been loaded)
     */
    public WorkingSchedule getWorkingSchedule() {
        return data.get().getWorkingSchedule();
    }
    
    public WorkingSchedule getSiteWorkingSchedule() {
        return data.get().getSiteWorkingSchedule();
    }
    
    /**
     * Activates time tracking for given case.
     * @param bugID
     * @throws FogBugzResponseTimeTrackingException if it isn't possible to starting time tracking for given case
     * @throws FogBugzException in case of other problems
     */
    public void startWork(CaseID bugID, IProgressMonitor monitor) throws FogBugzResponseTimeTrackingException, FogBugzException {
        request.requestAPI(command("startWork", "ixBug", bugID.toString()), monitor);
    }
    
    /**
     * Deactivates time tracking for current case.
     * @param monitor 
     */
    public void stopWork(IProgressMonitor monitor) throws FogBugzException {
        request.requestAPI(command("stopWork"), monitor);
    }

    public PersonID getOwner(ProjectID projectID, AreaID areaID) {
        return this.data.get().getOwner(projectID, areaID);
    }
    
    public int getApiVersion() {
        return apiVersion;
    }
    
    /**
     * @param urlComponent URL as returned by Fogbugz (i.e. usually "default.asp?..." without host and token).
     * @throws FogBugzCommunicationException 
     */
    public void getAttachmentContent(String urlComponent, OutputStream output, IProgressMonitor monitor) throws FogBugzException {
        String url = getFullAttachmentUrlWithHostAndToken(urlComponent);

        request.download(url, output, monitor);
    }
    
    public void postNewAttachment(CaseID caseID, String comment, AttachmentData attachment, IProgressMonitor monitor) throws FogBugzException {
        ChangeEventData ced = new ChangeEventData();
        ced.setNewComment(comment);
        ced.setCaseID(caseID);
        ced.addAttachment(attachment);
        
        performCaseAction(CaseAction.EDIT, ced, monitor, false);
    }

    /**
     * @param action to be performed on case
     * @param data what to modify in case
     * @param loadEvents should new/updated case have all events information?
     * @return new or updated case with all details (with/without events as specified by loadEvents parameter)
     */
    public FogBugzCase performCaseAction(CaseAction action, ChangeEventData data, IProgressMonitor monitor, boolean loadEvents) throws FogBugzException {
        List<Part> parts = new ArrayList<Part>();
        parts.add(stringPart("cmd", action.getCommand()));
        parts.add(stringPart("token", getToken()));
        parts.add(stringPart("cols", getColumns(loadEvents))); // send back all details about case
        
        addOptionalPart(parts, "ixBug", data.getCaseID());
        addOptionalPart(parts, "ixBugEvent", data.getEventID());
        addOptionalPart(parts, "sTitle", data.getNewTitle());
        addOptionalPart(parts, "ixProject", data.getNewProjectID());
        addOptionalPart(parts, "ixArea", data.getNewAreaID());
        addOptionalPart(parts, "ixFixFor", data.getNewFixForID());
        addOptionalPart(parts, "ixCategory", data.getNewCategoryID());
        addOptionalPart(parts, "ixPersonAssignedTo", data.getNewAssignedTo());
        addOptionalPart(parts, "ixPriority", data.getNewPriorityID());
        addOptionalDatePart(parts, "dtDue", data.getNewDueDate());
        addOptionalPart(parts, "sVersion", data.getNewVersion());
        addOptionalPart(parts, "sComputer", data.getNewComputer());
        // sCustomerEmail, ixMailbox, sScoutDescription, sScoutMessage, fScoutStopReporting
        addOptionalPart(parts, "sEvent", data.getNewComment());
        addOptionalPart(parts, "ixStatus", data.getNewStatus());
        addOptionalPart(parts, "hrsCurrEst", convertDaysHoursMinutesToString(data.getNewCurrentHoursEstimate()));
        if (isFogBugz7Repository()) {
            addOptionalPart(parts, "ixBugParent", data.getParentCaseID());
            addOptionalPart(parts, "sTags", convertToTags(data.getTags()));
        }
        
        // hrsElapsed cannot be modified :-(
        // addOptionalPart(parts, "hrsElapsed", convertDaysHoursMinutesToString(data.getNewElapsedTime()));
        
        if (!data.getAttachments().isEmpty()) {
            int count = data.getAttachments().size();
            parts.add(stringPart("nFileCount", Integer.toString(count)));
            
            int fileIndex = 1;
            for (AttachmentData ad: data.getAttachments()) {
                parts.add(filePart("File" + fileIndex, ad));
                fileIndex ++;
            }
        }
        
        Document doc = request.post(getPostURL(), parts, monitor);
        
        Element caseElement = XOMUtils.xpathElement(doc, "/response/case");
        if (caseElement == null) {
            return null;
        }

        return createCase(caseElement, monitor, loadEvents);
    }

    private String convertToTags(List<String> tags) {
        if (tags == null) {
            return null;
        }

        if (tags.isEmpty()) {
            // Single space clears tags. See http://our.fogbugz.com/default.asp?fogbugz.4.84558.0 for details.
            return " ";
        }
        
        StringBuilder result = new StringBuilder();
        String delim = "";
        for (String t: tags) {
            result.append(delim);
            result.append(t);
            delim = ",";
        }
        
        return result.toString();
    }

    private String convertDaysHoursMinutesToString(DaysHoursMinutes e) {
        if (e == null) {
            return null;
        }

        e = e.normalize(getSiteWorkingSchedule().getWorkingHoursPerDay());
        
        StringBuilder sb = new StringBuilder();
        if (e.days.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(e.days.toPlainString());
            sb.append("d");
        }
        
        if (e.hours.compareTo(BigDecimal.ZERO) > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            
            sb.append(e.hours.toPlainString());
            sb.append("h");
        }
        
        if (e.minutes.compareTo(BigDecimal.ZERO) > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            
            sb.append(e.minutes.toPlainString());
            sb.append("m");
        }
        
        if (sb.length() == 0) {
            sb.append("0");
        }
        return sb.toString();
    }

    private void addOptionalDatePart(List<Part> parts, String name, Date value) {
        if (value == null) {
            return;
        }
        
        String formatted = Parsers.formatDate(value);
        parts.add(stringPart(name, String.valueOf(formatted)));
    }

    private void addOptionalPart(List<Part> parts, String name, Object value) {
        if (value != null) {
            parts.add(stringPart(name, String.valueOf(value)));
        }
    }
    
    private FilePart filePart(String name, AttachmentData attachment) {
        FilePart p = new FilePart(name, attachment.getPartSource(), attachment.getContentType(), null);
        p.setTransferEncoding(null);
        return p;
    }

    private StringPart stringPart(String name, String value) {
        // FogBugz doesn't like when we set Content-Type or Transfer-Enconding headers for part
        // It also always expects text encoded in UTF-8
        StringPart p = new StringPart(name, value, "UTF-8");
        p.setContentType(null);
        p.setTransferEncoding(null);
        return p;
    }
    
    public void loadCaches(IProgressMonitor monitor) throws FogBugzException {
        loadCaches(EnumSet.allOf(CacheType.class), monitor);
    }
    
    public void loadCaches(Set<CacheType> types, IProgressMonitor monitor) throws FogBugzException {
        Utils.checkCancellation(monitor);
        
        FogBugzDataBuilder builder = new FogBugzDataBuilder(data.get());
        
        if (types.contains(CacheType.CATEGORY)) {
            monitor.subTask("Loading categories");
            builder.addCategories(fetch(monitor, "listCategories", "/response/categories/category", new CategoryMapper()));
            monitor.worked(1);
        }
        
        Utils.checkCancellation(monitor);

        if (types.contains(CacheType.PRIORITY)) {
            monitor.subTask("Loading priorities");
            builder.addPriorities(fetch(monitor, "listPriorities", "/response/priorities/priority", new PriorityMapper()));
            monitor.worked(1);
        }
        
        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.STATUS)){
            monitor.subTask("Loading statuses");
            List<FogBugzStatus> resolved = fetch(monitor, "listStatuses", "/response/statuses/status", new StatusMapper(), "fResolved", "1");
            Set<StatusID> resolvedStatuses = new HashSet<StatusID>();
            for (FogBugzStatus fbs: resolved) {
                resolvedStatuses.add(fbs.getID());
            }
            
            builder.addStatuses(fetch(monitor, "listStatuses", "/response/statuses/status", new StatusMapper(resolvedStatuses)));
            monitor.worked(1);
        }

        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.PERSON)) {
            monitor.subTask("Loading people");
            
            List<FogBugzPerson> normalPeople = fetch(monitor, "listPeople", "/response/people/person", new PersonMapper());
            List<FogBugzPerson> virtualPeople = fetch(monitor, "listPeople", "/response/people/person", new PersonMapper(), "fIncludeVirtual", "1");
            
            List<FogBugzPerson> allPeople = new ArrayList<FogBugzPerson>();
            allPeople.addAll(normalPeople);
            allPeople.addAll(virtualPeople);
            
            builder.addPeople(allPeople);
            
            // get current user
            FogBugzPerson currentUser = fetchSingle(request, command("viewPerson"), "/response/person", monitor, new PersonMapper());
            if (currentUser != null) {
                builder.setCurrentUser(currentUser.getID());
            }
            
            monitor.worked(1);
        }

        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.AREA)) {
            monitor.subTask("Loading areas");
            builder.addAreas(fetch(monitor, "listAreas", "/response/areas/area", new AreaMapper()));
            monitor.worked(1);
        }

        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.PROJECT)) {
            monitor.subTask("Loading projects");
            builder.addProjects(fetch(monitor, "listProjects", "/response/projects/project", new ProjectMapper()));
            monitor.worked(1);
        }

        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.FIX_FOX)) {
            monitor.subTask("Loading releases");
            builder.addFixFors(fetch(monitor, "listFixFors", "/response/fixfors/fixfor", new FixForMapper(), "fIncludeDeleted", "1"));
            monitor.worked(1);
        }
        
        Utils.checkCancellation(monitor);
        
        if (types.contains(CacheType.WORKING_SCHEDULE)) {
            monitor.subTask("Loading working schedule");

            // There was bug in older fogbugz version, which prevented fogbugz from returning workingSchedule in some cases.
            builder.setWorkingSchedule(fetchSingle(request, command("listWorkingSchedule"), "/response/workingSchedule", monitor, new WorkingScheduleMapper()));
            builder.setSiteWorkingSchedule(fetchSingle(request, command("listWorkingSchedule", "ixPerson", "1"), "/response/workingSchedule", monitor, new WorkingScheduleMapper()));
            
            monitor.worked(1);
        }
        
        builder.addLoadCacheTypes(types);
        
        FogBugzData data = builder.build();
        this.data.set(data);
    }

    private static final class CategoryMapper implements Mapper<FogBugzCategory> {
        public FogBugzCategory mapElement(Element e) throws FogBugzException {
            CategoryID id = CategoryID.valueOf(XOMUtils.xpathValueOf(e, "ixCategory"));
            String name = XOMUtils.xpathValueOf(e, "sCategory");
            String namePlural = XOMUtils.xpathValueOf(e, "sPlural");
            boolean scheduleItem = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fIsScheduleItem"));
            StatusID defaultStatus = StatusID.valueOf(XOMUtils.xpathValueOf(e, "ixStatusDefault"));
            
            StatusID defaultActiveStatus = null;
            String defaultActiveStatusValue = XOMUtils.xpathValueOf(e, "ixStatusDefaultActive");
            if (defaultActiveStatusValue.trim().length() > 0) {
                defaultActiveStatus = StatusID.valueOf(defaultActiveStatusValue);
            }

            return new FogBugzCategory(id, name, namePlural, scheduleItem, defaultStatus, defaultActiveStatus);
        }
    }
    
    private static final class PriorityMapper implements Mapper<FogBugzPriority> {
        public FogBugzPriority mapElement(Element e) throws FogBugzException {
            PriorityID pid = PriorityID.valueOf(XOMUtils.xpathValueOf(e, "ixPriority"));
            String name = XOMUtils.xpathValueOf(e, "sPriority");
            boolean isDefault = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fDefault"));
            
            return new FogBugzPriority(pid, name, isDefault);
        }
    }

    private static final class StatusMapper implements Mapper<FogBugzStatus> {
        private final Set<StatusID> resolvedStatuses;
        
        StatusMapper(Set<StatusID> resolvedStatuses) {
            this.resolvedStatuses = resolvedStatuses;
        }

        StatusMapper() {
            this(Collections.<StatusID>emptySet());
        }
        
        public FogBugzStatus mapElement(Element e) throws FogBugzException {
            StatusID id = StatusID.valueOf(XOMUtils.xpathValueOf(e, "ixStatus"));
            String name = XOMUtils.xpathValueOf(e, "sStatus");
            CategoryID cat = CategoryID.valueOf(XOMUtils.xpathValueOf(e, "ixCategory"));

            boolean resolved = resolvedStatuses.contains(id);
            int order = -1;
            String orderValue = XOMUtils.xpathValueOf(e, "iOrder");
            try {
                order = Integer.parseInt(orderValue);
            } catch (NumberFormatException ex) {
                // ignore
            }
            
            return new FogBugzStatus(id, name, cat, resolved, order);
        }
    }
    
    private static final class PersonMapper implements Mapper<FogBugzPerson> {
        public FogBugzPerson mapElement(Element e) {
            PersonID personID = PersonID.valueOf(XOMUtils.xpathValueOf(e, "ixPerson"));
            String fullName = XOMUtils.xpathValueOf(e, "sFullName");
            String email = XOMUtils.xpathValueOf(e, "sEmail");
            boolean inactive = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fDeleted"));
            boolean virtual = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fVirtual"));

            return new FogBugzPerson(personID, fullName, email, virtual, inactive);
        }
    }

    private static class ProjectMapper implements Mapper<FogBugzProject> {
        public FogBugzProject mapElement(Element e) {
            ProjectID projectID = ProjectID.valueOf(XOMUtils.xpathValueOf(e, "ixProject"));
            String name = XOMUtils.xpathValueOf(e, "sProject");
            boolean inbox = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fInbox"));
            PersonID ownerID = PersonID.valueOf(XOMUtils.xpathValueOf(e, "ixPersonOwner"));

            return new FogBugzProject(projectID, name, inbox, ownerID);
        }
    }

    private static class AreaMapper implements Mapper<FogBugzArea> {
        public FogBugzArea mapElement(Element e) {
            AreaID areaID = AreaID.valueOf(XOMUtils.xpathValueOf(e, "ixArea"));
            String name = XOMUtils.xpathValueOf(e, "sArea");
            ProjectID projectID = ProjectID.valueOf(XOMUtils.xpathValueOf(e, "ixProject"));
            
            PersonID ownerID = null;
            String owner = XOMUtils.xpathValueOf(e, "ixPersonOwner");
            if (owner.trim().length() > 0) {
                ownerID = PersonID.valueOf(owner);
            }

            return new FogBugzArea(areaID, name, projectID, ownerID);
        }
    }
    
    private class CaseMapper implements Mapper<FogBugzCase> {
        private final IProgressMonitor monitor;
        private final boolean loadEvents;
        
        CaseMapper(IProgressMonitor monitor, boolean fullDetails) {
            this.monitor = monitor;
            this.loadEvents = fullDetails;
        }
        
        public FogBugzCase mapElement(Element e) throws FogBugzException {
            return createCase(e, monitor, loadEvents);
        }
    }
    
    private static class FixForMapper implements Mapper<FogBugzFixFor> {
        public FogBugzFixFor mapElement(Element e) throws FogBugzException {
            FixForID id = FixForID.valueOf(XOMUtils.xpathValueOf(e, "ixFixFor"));
            String name = XOMUtils.xpathValueOf(e, "sFixFor");
            boolean deleted = Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fDeleted"));
            Date date = Parsers.parseDate(XOMUtils.xpathValueOf(e, "dt"));
            ProjectID projectID = null;
            String pid = XOMUtils.xpathValueOf(e, "ixProject");
            if (pid.trim().length() > 0) {
                projectID = ProjectID.valueOf(pid);
            }
            
            return new FogBugzFixFor(id, name, deleted, date, projectID);
        }
    }

    private static class WorkingScheduleMapper implements Mapper<WorkingSchedule> {
        public WorkingSchedule mapElement(Element e) throws FogBugzException {
            WorkingSchedule ws = new WorkingSchedule();
            
            ws.setWorkdayStart(new BigDecimal(XOMUtils.xpathValueOf(e, "nWorkdayStarts")));
            ws.setWorkdayEnd(new BigDecimal(XOMUtils.xpathValueOf(e, "nWorkdayEnds")));
            ws.setHasLunch(Boolean.parseBoolean(XOMUtils.xpathValueOf(e, "fHasLunch")));
            ws.setLunchStart(Parsers.parseHours(XOMUtils.xpathValueOf(e, "nLunchStarts"), true));
            ws.setLunchLenghtHours(Parsers.parseHours(XOMUtils.xpathValueOf(e, "hrsLunchLength"), true));
            
            return ws;
        }
    }

    /**
     * FogBugz 7 introduced subcases and tags feature.
     */
    public boolean isFogBugz7Repository() {
        return apiVersion >= 7;
    }
    
    public PersonID getCurrentUser() {
        return data.get().getCurrentUser();
    }
}
