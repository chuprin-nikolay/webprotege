package edu.stanford.bmir.protege.web.server.form;

import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.dispatch.AbstractHasProjectActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.hierarchy.AssertedClassHierarchyProvider;
import edu.stanford.bmir.protege.web.shared.form.*;
import edu.stanford.bmir.protege.web.shared.form.data.FormDataPrimitive;
import edu.stanford.bmir.protege.web.shared.form.field.*;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static edu.stanford.bmir.protege.web.shared.form.field.ChoiceDescriptor.choice;
import static edu.stanford.bmir.protege.web.shared.form.field.ChoiceFieldType.COMBO_BOX;
import static java.util.Arrays.asList;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 07/04/16
 */
public class GetFormDescriptorActionHander extends AbstractHasProjectActionHandler<GetFormDescriptorAction, GetFormDescriptorResult> {

    private final ProjectId projectId;

    private final AssertedClassHierarchyProvider classHierarchyProvider;

    private final OWLDataFactory dataFactory;

    private final FormDataRepository formDataRepository;

    @Inject
    public GetFormDescriptorActionHander(@Nonnull AccessManager accessManager,
                                         ProjectId projectId,
                                         AssertedClassHierarchyProvider classHierarchyProvider,
                                         OWLDataFactory dataFactory, FormDataRepository formDataRepository) {
        super(accessManager);
        this.projectId = projectId;
        this.classHierarchyProvider = classHierarchyProvider;
        this.dataFactory = dataFactory;
        this.formDataRepository = formDataRepository;
    }


    @Override
    public Class<GetFormDescriptorAction> getActionClass() {
        return GetFormDescriptorAction.class;
    }

    public GetFormDescriptorResult execute(GetFormDescriptorAction action, ExecutionContext executionContext) {
        return getDummy(action.getSubject());
    }


    private GetFormDescriptorResult getDummy(OWLEntity entity) {
        if (!entity.isOWLClass()) {
            return new GetFormDescriptorResult(projectId, entity, FormDescriptor.empty(), FormData.empty());
        }

        FormDescriptor.Builder builder = FormDescriptor.builder(new FormId("Class"));
        FormElementId labelFieldId = new FormElementId("Label");
        builder.addDescriptor(new FormElementDescriptor(
                labelFieldId,
                "Label",
                new TextFieldDescriptor(
                        "Enter label",
                        StringType.SIMPLE_STRING,
                        LineMode.SINGLE_LINE,
                        ".+",
                        "Please enter a non-empty label"
                ),
                Repeatability.NON_REPEATABLE,
                Required.REQUIRED,
                ""
        ));
        FormElementId altLabelFieldId = new FormElementId("synonyms");
        builder.addDescriptor(new FormElementDescriptor(
                altLabelFieldId,
                "Synonyms",
                new CompositeFieldDescriptor(
                        asList(
                                new CompositeFieldDescriptorEntry(
                                        new FormElementId("synonym"), 1, 1,
                                        new TextFieldDescriptor(
                                                "Enter synonym",
                                                StringType.LANG_STRING,
                                                LineMode.SINGLE_LINE,
                                                "",
                                                ""
                                        )),
                                new CompositeFieldDescriptorEntry(
                                        new FormElementId("synonymType"), 0, 0,
                                        new ChoiceFieldDescriptor(
                                                COMBO_BOX,
                                                asList(
                                                        choice("Exact", FormDataPrimitive.get("EXACT")),
                                                        choice("Narrower", FormDataPrimitive.get("NARROWER")),
                                                        choice("Broader", FormDataPrimitive.get("BROADER"))
                                                )
                                        )

                                )
                        ))
                ,
                Repeatability.REPEATABLE_VERTICAL,
                Required.OPTIONAL,
                ""
        ));
        FormElementId definitionFieldId = new FormElementId("definition");
        builder.addDescriptor(new FormElementDescriptor(
                definitionFieldId,
                "Definition",
                new TextFieldDescriptor(
                        "Enter label",
                        StringType.SIMPLE_STRING,
                        LineMode.SINGLE_LINE,
                        "",
                        ""
                ),
                Repeatability.NON_REPEATABLE,
                Required.OPTIONAL,
                ""
        ));
        FormElementId statusFieldId = new FormElementId("productionStatus");
        builder.addDescriptor(new FormElementDescriptor(
                                      statusFieldId,
                                      "Production Status",
                                      new ChoiceFieldDescriptor(
                                              COMBO_BOX,
                                              asList(
                                                      choice("In Production", FormDataPrimitive.get("InProduction")),
                                                      choice("Out of Production", FormDataPrimitive.get("OutOfProduction"))
                                              )),
                                      Repeatability.NON_REPEATABLE,
                                      Required.REQUIRED,
                                      ""
                              )
        );
        FormElementId depictionFieldId = new FormElementId("depiction");
        builder.addDescriptor(new FormElementDescriptor(
                depictionFieldId,
                "Depictions",
                new ImageFieldDescriptor(),
                Repeatability.REPEATABLE_HORIZONTAL,
                Required.OPTIONAL,
                ""
        ));

        FormElementId manufacturerField = new FormElementId("manufacturer");
        List<ChoiceDescriptor> manufacturerChoices;
        manufacturerChoices = asList(
                choice("Boeing",
                       FormDataPrimitive.get(dataFactory.getOWLNamedIndividual(IRI.create(
                               "http://webprotege.stanford.edu/Boeing")))),
                choice("Airbus",
                       FormDataPrimitive.get(dataFactory.getOWLNamedIndividual(IRI.create(
                               "http://webprotege.stanford.edu/AirbusIndustrie")))),
                choice("Lockheed",
                       FormDataPrimitive.get(dataFactory.getOWLNamedIndividual(IRI.create(
                               "http://webprotege.stanford.edu/AirbusIndustrie"))))
        );
        builder.addDescriptor(new FormElementDescriptor(
                manufacturerField,
                "Manufacturer",
                new ChoiceFieldDescriptor(
                        COMBO_BOX,
                        manufacturerChoices
                ),
                Repeatability.NON_REPEATABLE,
                Required.REQUIRED,
                ""
        ));

        List<CompositeFieldDescriptorEntry> childEntries = new ArrayList<>();
        childEntries.add(new CompositeFieldDescriptorEntry(
                new FormElementId("firstName"),
                1, 1,
                new TextFieldDescriptor(
                        "Enter first name",
                        StringType.SIMPLE_STRING,
                        LineMode.SINGLE_LINE,
                        "",
                        ""
                )
        ));
        childEntries.add(new CompositeFieldDescriptorEntry(
                new FormElementId("lastName"),
                1, 1,
                new TextFieldDescriptor(
                        "Enter last name",
                        StringType.SIMPLE_STRING,
                        LineMode.SINGLE_LINE,
                        "",
                        ""
                )
        ));
        childEntries.add(new CompositeFieldDescriptorEntry(
                new FormElementId("role"),
                0, 0,
                new ChoiceFieldDescriptor(
                        COMBO_BOX,
                        asList(
                                choice("Chief designer", FormDataPrimitive.get(IRI.create("Chief"))),
                                choice("Assistant designer", FormDataPrimitive.get(IRI.create("Assistant"))),
                                choice("Junior designer", FormDataPrimitive.get(IRI.create("Junior")))
                        )
                )
        ));
        CompositeFieldDescriptor designerField = new CompositeFieldDescriptor(childEntries);
        builder.addDescriptor(new FormElementDescriptor(
                new FormElementId("Designer"),
                "Designer",
                designerField,
                Repeatability.REPEATABLE_VERTICAL,
                Required.REQUIRED,
                ""
        ));

        FormData formData = formDataRepository.get(entity);
        System.out.println("Got form data from repository for entity: " + entity + " ---> " + formData);
        return new GetFormDescriptorResult(
                projectId,
                entity,
                builder.build(),
                formData
        );
    }
}
