package com.impossibl.postgres.protocol;

import com.impossibl.postgres.system.Context;

import static com.impossibl.postgres.system.Empty.EMPTY_FIELDS;
import static com.impossibl.postgres.utils.Nulls.firstNonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import static java.util.Collections.emptyIterator;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;

public class ResultBatch extends AbstractReferenceCounted implements Iterable<ResultBatch.Row>, AutoCloseable {

  public static class Row {

    private ResultField[] fields;
    private RowData rowData;

    private Row(ResultField[] fields, RowData rowData) {
      this.fields = fields;
      this.rowData = rowData;
    }

    public RowData getData() {
      return rowData;
    }

    public Object getField(int fieldIndex, Context context, Class<?> targetClass, Object targetContext) throws IOException {
      return rowData.getField(fieldIndex, fields[fieldIndex], context, targetClass, targetContext);
    }

    public <T> T getField(int fieldIndex, Context context, Class<T> targetClass) throws IOException {
      return targetClass.cast(getField(fieldIndex, context, targetClass, null));
    }

  }

  private class RowIterator implements Iterator<Row> {

    private Iterator<RowData> resultsIterator;

    private RowIterator(Iterator<RowData> resultsIterator) {
      this.resultsIterator = resultsIterator;
    }

    @Override
    public boolean hasNext() {
      return resultsIterator.hasNext();
    }

    @Override
    public Row next() {
      return new Row(fields, resultsIterator.next());
    }

  }

  private String command;
  private Long rowsAffected;
  private Long insertedOid;
  private ResultField[] fields;
  private RowDataSet rows;

  public ResultBatch(String command, Long rowsAffected, Long insertedOid, ResultField[] fields, RowDataSet rows) {
    this.command = command;
    this.rowsAffected = rowsAffected;
    this.insertedOid = insertedOid;
    this.fields = firstNonNull(fields, EMPTY_FIELDS);
    this.rows = rows;
  }

  public boolean hasRows() {
    return fields.length != 0;
  }

  public boolean isEmpty() {
    return !hasRows() || rows.isEmpty();
  }

  public String getCommand() {
    return command;
  }

  public boolean hasRowsAffected() {
    return rowsAffected != null;
  }

  public Long getRowsAffected() {
    if (!hasRowsAffected()) return null;
    return firstNonNull(rowsAffected, 0L);
  }

  public Long getInsertedOid() {
    return insertedOid;
  }

  public ResultField[] getFields() {
    return fields;
  }

  public RowDataSet borrowRows() {
    return rows;
  }

  public RowDataSet takeRows() {
    RowDataSet rows = this.rows;
    this.rows = null;
    this.fields = EMPTY_FIELDS;
    return rows;
  }

  public void clearRowsAffected() {
    this.rowsAffected = null;
  }

  public void clearRows() {
    ReferenceCountUtil.release(takeRows());
    fields = EMPTY_FIELDS;
  }

  public Row getRow(int rowIndex) {
    return new Row(fields, rows.borrow(rowIndex));
  }

  @Override
  protected void deallocate() {
    if (rows != null) {
      rows.release();
    }
  }

  public ResultBatch touch(Object hint) {
    if (rows != null) {
      rows.touch(hint);
    }
    return this;
  }

  @Override
  public Iterator<Row> iterator() {
    if (rows == null) return emptyIterator();
    return new RowIterator(rows.borrowAll().iterator());
  }

  @Override
  public void close() {
    release();
  }

  @Override
  public String toString() {
    return "ResultBatch{" +
        "command='" + command + '\'' +
        ", rowsAffected=" + rowsAffected +
        ", insertedOid=" + insertedOid +
        ", fields=" + Arrays.toString(fields) +
        ", rows=" + rows +
        '}';
  }
}