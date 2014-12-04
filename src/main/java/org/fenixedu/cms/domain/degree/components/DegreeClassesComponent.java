package org.fenixedu.cms.domain.degree.components;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static net.sourceforge.fenixedu.domain.ExecutionSemester.readActualExecutionSemester;
import static net.sourceforge.fenixedu.domain.SchoolClass.COMPARATOR_BY_NAME;
import static net.sourceforge.fenixedu.domain.person.RoleType.COORDINATOR;
import static net.sourceforge.fenixedu.domain.person.RoleType.RESOURCE_ALLOCATION_MANAGER;
import static net.sourceforge.fenixedu.util.PeriodState.NOT_OPEN;
import static net.sourceforge.fenixedu.util.PeriodState.OPEN;
import static org.fenixedu.bennu.core.security.Authenticate.getUser;
import static pt.ist.fenixframework.FenixFramework.getDomainObject;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import net.sourceforge.fenixedu.dataTransferObject.InfoDegree;
import net.sourceforge.fenixedu.domain.*;

import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;

import com.google.common.collect.Sets;

/**
 * Created by borgez on 10/9/14.
 */
@ComponentType(name = "Degree classes", description = "Schedule of a class")
public class DegreeClassesComponent extends DegreeSiteComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext global) {
        Degree degree = degree(page);
        global.put("degreeInfo", InfoDegree.newInfoFromDomain(degree));

        ExecutionSemester selectedSemester = getExecutionSemester(global.getRequestContext());
        ExecutionSemester otherSemester = getOtherExecutionSemester(selectedSemester);

        SortedMap<Integer, Set<SchoolClass>> selectedClasses = classesByCurricularYear(degree, selectedSemester);
        SortedMap<Integer, Set<SchoolClass>> nextClasses = classesByCurricularYear(degree, otherSemester);
        global.put("classesByCurricularYearAndSemesters", of(selectedSemester, selectedClasses, otherSemester, nextClasses));
    }

    private SortedMap<Integer, Set<SchoolClass>> classesByCurricularYear(Degree degree, ExecutionSemester semester) {
        DegreeCurricularPlan plan = degree.getMostRecentDegreeCurricularPlan();
        Predicate<SchoolClass> predicate = schoolClass -> schoolClass.getExecutionDegree().getDegreeCurricularPlan() == plan;
        return semester.getSchoolClassesSet().stream().filter(predicate).collect(
                groupingBy(SchoolClass::getAnoCurricular, TreeMap::new, toCollection(() -> Sets.newTreeSet(COMPARATOR_BY_NAME))));
    }

    private ExecutionSemester getOtherExecutionSemester(ExecutionSemester semester) {
        ExecutionSemester next = semester.getNextExecutionPeriod();
        return canViewNextExecutionSemester(next) ? next : semester.getPreviousExecutionPeriod();
    }

    private boolean canViewNextExecutionSemester(ExecutionSemester nextExecutionSemester) {
        Predicate<Person> hasPerson = person -> person != null;
        Predicate<Person> isCoordinator = person -> person.hasRole(COORDINATOR);
        Predicate<Person> isManager = person -> person.hasRole(RESOURCE_ALLOCATION_MANAGER); 
        return nextExecutionSemester.getState() == OPEN || 
                (nextExecutionSemester.getState() == NOT_OPEN && getUser() != null 
                        && hasPerson.and(isManager.or(isCoordinator)).test(getUser().getPerson()));
    }

    private ExecutionSemester getExecutionSemester(String[] requestContext) {
        return requestContext.length > 2 ? getDomainObject(requestContext[1]) : readActualExecutionSemester();
    }
}
