/**
 * Copyright (c) 2013, impossibl.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of impossibl.com nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.impossibl.postgres.jdbc;

import com.impossibl.postgres.protocol.PrepareCommand;
import com.impossibl.postgres.protocol.QueryCommand;
import com.impossibl.postgres.types.Type;
import static com.impossibl.postgres.jdbc.ErrorUtils.chainWarnings;
import static com.impossibl.postgres.jdbc.Exceptions.INVALID_COMMAND_FOR_GENERATED_KEYS;
import static com.impossibl.postgres.jdbc.Exceptions.NOT_SUPPORTED;
import static com.impossibl.postgres.jdbc.Exceptions.NO_RESULT_COUNT_AVAILABLE;
import static com.impossibl.postgres.jdbc.Exceptions.NO_RESULT_SET_AVAILABLE;
import static com.impossibl.postgres.jdbc.SQLTextUtils.appendReturningClause;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Arrays.asList;

class PGSimpleStatement extends PGStatement {

  SQLText batchCommands;

  public PGSimpleStatement(PGConnection connection, int type, int concurrency, int holdability) {
    super(connection, type, concurrency, holdability, null, null);
  }

  SQLWarning prepare(SQLText sqlText) throws SQLException {

    PrepareCommand prep = connection.getProtocol().createPrepare(null, sqlText.toString(), Collections.<Type>emptyList());

    SQLWarning warningChain = connection.execute(prep, true);

    resultFields = prep.getDescribedResultFields();

    return warningChain;
  }

  boolean execute(SQLText sqlText) throws SQLException {

    if (processEscapes) {
      SQLTextEscapes.processEscapes(sqlText, connection);
    }

    if (sqlText.getStatementCount() > 1) {

      return executeSimple(sqlText.toString());

    }
    else {

      SQLWarning prepWarningChain = prepare(sqlText);

      boolean res = executeStatement(null, Collections.<Type>emptyList(), Collections.<Object>emptyList());

      warningChain = chainWarnings(prepWarningChain, warningChain);

      return res;

    }
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    checkClosed();

    SQLText sqlText = connection.parseSQL(sql);

    return execute(sqlText);
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    checkClosed();

    SQLText sqlText = connection.parseSQL(sql);

    if (autoGeneratedKeys != RETURN_GENERATED_KEYS) {
      return execute(sqlText);
    }

    if (!appendReturningClause(sqlText)) {
      throw INVALID_COMMAND_FOR_GENERATED_KEYS;
    }

    execute(sqlText);

    generatedKeysResultSet = getResultSet();

    return false;
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    checkClosed();

    throw NOT_SUPPORTED;
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    checkClosed();

    SQLText sqlText = connection.parseSQL(sql);

    if (!appendReturningClause(sqlText, asList(columnNames))) {
      throw INVALID_COMMAND_FOR_GENERATED_KEYS;
    }

    execute(sqlText);

    generatedKeysResultSet = getResultSet();

    return false;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {

    if (!execute(sql)) {
      throw NO_RESULT_SET_AVAILABLE;
    }

    return getResultSet();
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {

    if (execute(sql)) {
      throw NO_RESULT_COUNT_AVAILABLE;
    }

    return getUpdateCount();
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {

    if (execute(sql, autoGeneratedKeys)) {
      throw NO_RESULT_COUNT_AVAILABLE;
    }

    return getUpdateCount();
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {

    if (execute(sql, columnIndexes)) {
      throw NO_RESULT_COUNT_AVAILABLE;
    }

    return getUpdateCount();
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {

    if (execute(sql, columnNames)) {
      throw NO_RESULT_COUNT_AVAILABLE;
    }

    return getUpdateCount();
  }

  @Override
  public void addBatch(String sql) throws SQLException {

    SQLText sqlText = connection.parseSQL(sql);

    if (batchCommands == null) {
      batchCommands = sqlText;
    }
    else {
      batchCommands.addStatements(sqlText);
    }
  }

  @Override
  public void clearBatch() throws SQLException {

    batchCommands = null;
  }

  @Override
  public int[] executeBatch() throws SQLException {

    try {

      if (batchCommands == null) {
        return new int[0];
      }

      execute(batchCommands);

      int[] counts = new int[resultBatches.size()];

      for (int c = 0; c < resultBatches.size(); ++c) {

        QueryCommand.ResultBatch resultBatch = resultBatches.get(c);

        if (resultBatch.command.equals("SELECT")) {
          throw new BatchUpdateException(Arrays.copyOf(counts, c));
        }

        if (resultBatch.rowsAffected != null) {
          counts[c] = (int)(long)resultBatches.get(c).rowsAffected;
        }
        else {
          counts[c] = Statement.SUCCESS_NO_INFO;
        }
      }

      return counts;
    }
    finally {

      batchCommands = null;
      command = null;
      resultBatches = null;
    }

  }

}
