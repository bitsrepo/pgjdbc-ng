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
package com.impossibl.postgres.system.procs;

import com.impossibl.postgres.jdbc.PGBlob;
import com.impossibl.postgres.jdbc.PGClob;
import com.impossibl.postgres.jdbc.PGDirectConnection;
import com.impossibl.postgres.system.Context;
import com.impossibl.postgres.system.ConversionException;
import com.impossibl.postgres.types.Type;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

public class Oids extends SimpleProcProvider {

  public Oids() {
    super(new TxtEncoder(), new TxtDecoder(), new BinEncoder(), new BinDecoder(), "oid");
  }

  static class BinDecoder extends UInt4s.BinDecoder {

    @Override
    protected Object convertOutput(Context context, Integer decoded, Class<?> targetClass, Object targetContext) throws IOException {

      if (Blob.class.isAssignableFrom(targetClass)) {
        try {
          return new PGBlob((PGDirectConnection) context.unwrap(), decoded);
        }
        catch (SQLException e) {
          throw new IOException(e);
        }
      }

      if (Clob.class.isAssignableFrom(targetClass)) {
        try {
          return new PGClob((PGDirectConnection) context.unwrap(), decoded);
        }
        catch (SQLException e) {
          throw new IOException(e);
        }
      }

      return super.convertOutput(context, decoded, targetClass, targetContext);
    }

  }

  static class BinEncoder extends UInt4s.BinEncoder {

    @Override
    protected Integer convertInput(Context context, Type type, Object source, Object sourceContext) throws ConversionException {

      if (source instanceof PGBlob) {
        return ((PGBlob) source).getOid();
      }

      if (source instanceof PGClob) {
        return ((PGClob) source).getOid();
      }

      return super.convertInput(context, type, source, sourceContext);
    }
  }

  static class TxtDecoder extends UInt4s.TxtDecoder {

    @Override
    protected Object convertOutput(Context context, Integer decoded, Class<?> targetClass, Object targetContext) throws IOException {

      if (Blob.class.isAssignableFrom(targetClass)) {
        try {
          return new PGBlob((PGDirectConnection) context.unwrap(), decoded);
        }
        catch (SQLException e) {
          throw new IOException(e);
        }
      }

      if (Clob.class.isAssignableFrom(targetClass)) {
        try {
          return new PGClob((PGDirectConnection) context.unwrap(), decoded);
        }
        catch (SQLException e) {
          throw new IOException(e);
        }
      }

      return super.convertOutput(context, decoded, targetClass, targetContext);
    }

  }

  static class TxtEncoder extends UInt4s.TxtEncoder {

    @Override
    protected Integer convertInput(Context context, Type type, Object source, Object sourceContext) throws ConversionException {

      if (source instanceof PGBlob) {
        return ((PGBlob) source).getOid();
      }

      if (source instanceof PGClob) {
        return ((PGClob) source).getOid();
      }

      return super.convertInput(context, type, source, sourceContext);
    }

  }

}
