/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction;

/**
 * Interface that specifies means to programmatically manage
 * transaction savepoints in a generic fashion.
 *
 * <p>Note that savepoints can only work within an active transaction.
 * Just use this programmatic savepoint handling for advanced needs;
 * else, a subtransaction with PROPAGATION_NESTED is preferable.
 *
 * <p>This interface is inspired by JDBC 3.0's Savepoint mechanism
 * but is independent from any specific persistence technology.
 *
 * @author Juergen Hoeller
 * @since 18.06.2004
 * @see TransactionDefinition#PROPAGATION_NESTED
 * @see java.sql.Savepoint
 * savepoint管理器
 */
public interface SavepointManager {

	/**
	 * Create a new savepoint. You can roll back to a specific savepoint
	 * via <code>rollbackToSavepoint</code>, and explicitly release a
	 * savepoint that you don't need anymore via <code>releaseSavepoint</code>.
	 * <p>Note that most transaction managers will automatically release
	 * savepoints at transaction completion.
	 * @return a savepoint object, to be passed into rollbackToSavepoint
	 * or releaseSavepoint
	 * @throws TransactionException if the savepoint could not be created,
	 * either because the backend does not support it or because the
	 * transaction is not in an appropriate state
	 * @see java.sql.Connection#setSavepoint
	 * 创建savepoint
	 */
	Object createSavepoint() throws TransactionException;

	/**
	 * Roll back to the given savepoint. The savepoint will be
	 * automatically released afterwards.
	 * @param savepoint the savepoint to roll back to
	 * @throws TransactionException if the rollback failed
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 * 回滚到savepoint
	 */
	void rollbackToSavepoint(Object savepoint) throws TransactionException;

	/**
	 * Explicitly release the given savepoint.
	 * <p>Note that most transaction managers will automatically release
	 * savepoints at transaction completion.
	 * <p>Implementations should fail as silently as possible if
	 * proper resource cleanup will still happen at transaction completion.
	 * @param savepoint the savepoint to release
	 * @throws TransactionException if the release failed
	 * @see java.sql.Connection#releaseSavepoint
	 * 释放savepoint
	 */
	void releaseSavepoint(Object savepoint) throws TransactionException;

}
