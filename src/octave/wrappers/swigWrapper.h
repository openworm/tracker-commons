void initWrapper();
int isNullHandle(int handle);
int isNoneHandle(int handle);

int load_from_file(const char *path);
void save_to_file(int selfHandle, const char *path);
int to_canon(int selfHandle);
int add(int selfHandle, int handle2);
int eq(int selfHandle, int handle2);
int units(int selfHandle);
int metadata(int selfHandle);
long num_worms(int selfHandle);
int worm_ids(int selfHandle);
int data_as_odict(int selfHandle);

int MU_create(const char *unitStr);
double MU_to_canon(int selfHandle, double value);
double MU_from_canon(int selfHandle, double value);
const char *MU_unit_string(int selfHandle);
const char *MU_canonical_unit_string(int selfHandle);
