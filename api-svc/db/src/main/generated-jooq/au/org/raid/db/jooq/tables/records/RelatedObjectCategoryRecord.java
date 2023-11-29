/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.RelatedObjectCategory;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RelatedObjectCategoryRecord extends UpdatableRecordImpl<RelatedObjectCategoryRecord> implements Record3<Integer, String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.related_object_category.id</code>.
     */
    public RelatedObjectCategoryRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_object_category.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>api_svc.related_object_category.uri</code>.
     */
    public RelatedObjectCategoryRecord setUri(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_object_category.uri</code>.
     */
    public String getUri() {
        return (String) get(1);
    }

    /**
     * Setter for <code>api_svc.related_object_category.schema_id</code>.
     */
    public RelatedObjectCategoryRecord setSchemaId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_object_category.schema_id</code>.
     */
    public Integer getSchemaId() {
        return (Integer) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, Integer> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, Integer> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return RelatedObjectCategory.RELATED_OBJECT_CATEGORY.ID;
    }

    @Override
    public Field<String> field2() {
        return RelatedObjectCategory.RELATED_OBJECT_CATEGORY.URI;
    }

    @Override
    public Field<Integer> field3() {
        return RelatedObjectCategory.RELATED_OBJECT_CATEGORY.SCHEMA_ID;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getUri();
    }

    @Override
    public Integer component3() {
        return getSchemaId();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getUri();
    }

    @Override
    public Integer value3() {
        return getSchemaId();
    }

    @Override
    public RelatedObjectCategoryRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public RelatedObjectCategoryRecord value2(String value) {
        setUri(value);
        return this;
    }

    @Override
    public RelatedObjectCategoryRecord value3(Integer value) {
        setSchemaId(value);
        return this;
    }

    @Override
    public RelatedObjectCategoryRecord values(Integer value1, String value2, Integer value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached RelatedObjectCategoryRecord
     */
    public RelatedObjectCategoryRecord() {
        super(RelatedObjectCategory.RELATED_OBJECT_CATEGORY);
    }

    /**
     * Create a detached, initialised RelatedObjectCategoryRecord
     */
    public RelatedObjectCategoryRecord(Integer id, String uri, Integer schemaId) {
        super(RelatedObjectCategory.RELATED_OBJECT_CATEGORY);

        setId(id);
        setUri(uri);
        setSchemaId(schemaId);
    }
}
