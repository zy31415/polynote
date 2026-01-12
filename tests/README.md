

## Install python library

```shell
poetry add --group test {python_lib}
```

## Run pytest

Run all test:
```shell
poetry run pytest
```

Run a specific test file:
```shell
poetry run pytest 'tests/cases/replication/test_update_conflicts.py::test_update_conflicts_tie_breaker'
```

### Control how ft testing deployment is handled after tests are done
`--cluster-teardown`

- `always`: Always teardown the cluster after tests are done (default).
- `never`: Never teardown the cluster after tests are done.
- `on-success`: Only teardown the cluster if any test fails.



## Debugging commdns

Show last 10,000 lines of logs for node B in the polynote-ft namespace:
```shell
kubectl logs -n polynote-ft -l node-id=b --tail=10000
```