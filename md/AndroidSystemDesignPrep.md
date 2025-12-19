# Android System Design

**c4 model**
1. System context diagram - High level diagram which talks about the mobile app as single box and its dependecies - emphasis on user, backend, push provider, connected devices, email, mainframes etc. (focus on what is outside the mobile app, yet relevant for the solution).
2. Container diagram - Zoom into mobile app and show details within, such as image loader, DI, storage service, network service, presentation, relevant flows.
3. Component diagram - Module level details where each item is an independent entity which can be developed in isolation. This helps in providing the vision of how a team can work on the problem. Here talk about functionality and divide each into modules contains repo, use cases, view models etc.

## Android and app fundamentals
Android OS is a layered system where application runs on top of managed runtime. All app lifecycle, process control is owned by the system. System can kill any app any time. 

At the bottom, there is a Linux kernel which provides primitives such as process scheduling, memory management, file system, networking, device drivers etc. Android biulds on top of this and provides a higher-level abstraction. Every app runs on a sandbox, with its own unique process id, memory allocation, which is isolated form other apps. Each app is in Android is its own user to the kernel, thereby making it easier for isolation.

Above the kernel we have ART (Android run time). This layer contains native deamons and libraries largely in C and C++. Example MediaServer, Binder's driver interface etc. ART is responsible for executing app bytecode, manage memory, optimize execution. It uses ahead-of-time and just-in-time compilation. ART is not started separately for each app, but Android uses a special system process called `Zygote` to host a pre-initialized runtime.

Zygote is started up during Android boot up. Zygote is a process factory. It initializes ART and preloads common framework classes, resources and native liberaries that almost every app would need. 

AMS (Activity Management Service) is the brain of Android which decides when a process should be created, which component should run, how the process fits into the overall system state. It tracks running processes, tasks, activities, services, broadcasts and is in charge of lifecycle transitions. AMS makes policy decisions such as if a process should be started, reused or killed, under which UID it runs, which component should be launched etc. 

Once AMS decides these and wants to launch an app, it uses Zygote. You can imagine Zygote to be a container waiting ready and when AMS tells it, it forks itself (duplicates itself) and that becomes the app process. Zygote goes back to waiting for next request completely detached from the forked child process, which is now under AMS control. 

```
AMS → Zygote socket → Zygote forks → new app process → ActivityThread starts
```

In most cases, every app has only single process. Everything inside of it are just classes in a JVM. Be it service, activity, receivers, all of these run in a single process. The way this works is using a Looper. More about that later.

There are cases where an app can have multiple processes when it wants to offload some heavy work to a separate process. In this case, a new app remote process is created using zygote using the same APK. This app remote process has its own process, memory, heap etc. The two will communicate using IPC (ALDL etc.)

From Android point of view app is a collection of capabilities / components which the AMS will activate and deactivate as needed. So a call like `startActivity` for starting an activity in an app will not create a screen, but it submits this request via IPC to AMS and then AMS will decide if it has to create a new process or reuse the existing process to do what is asked for.

### Looper and threads
Android app is not single threaded. It is event loop driven on specific thread. Main thread is a normal linux thread which is created when app is forked and launched by the zygote and AMS. It has a looper and it owns UI state. Main thread spins in an infinite loop, pulling messages from message queue and executing them one at a time. Acticity lifecycle callbacks, input events (touch, key), view invalidation and layout passes, `Handler.post{}`, `runOnUiThread{}` etc are messages in the message queue. When these callbacks or messages get executed, inside there, you could have something like this:

```
lifecycleScope.launch {
    // By default this uses Dispatchers.Main
    val data = withContext(Dispatchers.IO) {
        fetchFromNetworkOrDB()
    }

    updateUI(data)
}
``` 

This launch is yet another message (runnable) in the message queue for main looper. When that executes in its event cycle, the code inside is asking for IO dispatcher. That time the block (runnable) is submitted to the IO thread pool, where it gets executed. This is how background threads come into play. They perform blocking work like network calls, disk IO or computation. Then they post the result back to the main thead. In our example, the coroutine will resume in MAIN dispatcher, otherwise one could use `Handler(Looper.getMainLooper()).post {}` which is old style. 

Mental model:
```
Threads execute code
Loopers serialize work on a thread
Executors manage pools of threads
Dispatchers choose executors or loopers
Coroutines split execution into resumable pieces
```

## UI patterns
### MVVM 
This is mode, view, view model. In this view observes view model. View model exposes reactive streams (live data, flow etc). View model does not know about view and hence its fully testable.
This is presently recommended by Google. This provides excellent lifecycle handling. It also works well with Jetpack compose. Since these data is split across multiple streams and updated async, it can lead to inconsitencies in the view. 

ViewModel should not hold Activity or fragment context. This will result in leaks. ViewModels potentially outlive the view, in case of device rotation or similar lifecycle events, view gets recreated, but viewmodel can be reused. Viewmodel can use `application context`. This can be used for resource loading, shared preferences access, DB / File IO or for anything that lives beyond the activity lifecycle. It is not OK to use for Toast, Dialog, starting activities etc. For those, ideally we use some signalling mechanism like `SingleLiveEvent` which triggers the logic to show the dialog or toast or launch the activity from the view layer.

Model refers to the entites, use case, repository and data source abstractions which feed the viewmodel with data.

```
View → ViewModel → UseCase → Repository → DataSource (network/db)
```

#### View model lifecycle and shared view models
Viewmodel is created using factory methods `viewModels()` or `activityViewModels` or `navGraphViewModels`. ViewModelStoreOwner defines the scope of a view model. Activity, Fragment, Nav Graph can all be ViewModelStoreOwners. Internally the owner exposes a ViewModelStore which holds view models against a key (which is class name) in a map. So whenever we call `viewModels()` or `activityViewModels` or `ViewModelProvider(owner).get(ViewMode::class.java)`, Android looks up the respective store and if there is an instance present, it returns, otherwise creates.

Viewmodels are not disposed during configuration changes. They are disposed only when the owner is destroyed. At this time `viewModel#clear()` is invoked.

The traditional way to pass data is by uisng bundle. Whenever an intent is created we can use `putExtra` to pass stuff around. This has to be serializable. A more modern approach is to use shared view model to share data. This often avoids mistakes and helps in sharing the complete state of the viewModel without much effort. A view model scoped to a parent UI can share state with multiple child fragments. In this case we create an viewmodel and use it between the activity and its child fragments. This can be injected by using `activityViewModels()` which uses the Activity as the ViewModelStoreOwner.  

#### Configuration changes
Configuration change can happen during actions like screen rotation, system dark mode, local or language change, font scale change, orientation change in split screen etc.

When this happens, UI (activity and fragments) are destroyed and recreated. Viewmodels are reused. To handle this properly, all the view state must be in viewmodel and not in view. 

In the case of dark mode switch, resources have to be reloaded: colors.xml, drawable-night, style-night etc. Make use of `SavedStateHandle` to keep transient UI details like scroll position, toggle states etc.

In the case of locale, the resources needs to be reloaded: strings.xml, layout direction, formats etc. This has to be handled before view inflation.

#### Use of SavedStateHandle in viewmodel
`SavedStateHandle` is a kv store associated with a ViewModel. Its backed by `Bundle` saved in `onSaveInstanceState` and restored when process restarts. An instance can be injected automatically when using factory to create viewModel in Activity or Fragment. `SavedStateHandle` can be used for saving state in case of process death. ViewModel survives config changes, but not process death.
Common UI restoration items like scroll position, selected tab, filter queries, input fields etc. can be strored here.

The `SavedStateHandle` writes keys into the bundle that is same as what is used in Activity or Fragment's `onSaveInstanceState`. Android writes this bundle into SavedStateRegistry. When process restrats, the bundle is restored and the viewModel will get a `SavedStateHandle` which has the old values populated. So viewModel only have to update the `SavedStateHandle` with whatever information that it needs after process death. All values stored should be primitives and serailzable objects.

Example usage
```
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val saved: SavedStateHandle
) : ViewModel() {

    val query = saved.getStateFlow("query", "")

    fun updateQuery(q: String) {
        saved["query"] = q
    }
}
```

From UI do this

```
binding.searchInput.doOnTextChanged {
    viewModel.updateQuery(it.toString())
}
```

### MVP
This is model view presenter. View remains passive and presenter contains the UI logic that updates view using callbacks. View delegates all work to presenter.

Since presenter is non Android code like java or kotlin, its decoupled and compeletely testable. But it is hard to handle deep androoid lifecycle events. 

This is more or less obsolete today.

### MVI
This is model view intent. The main focus in this is to have immutable state and unidirectional data flow. There is a single state object that represents an entire screen. View emits intents, logic reduces this into new state and that renderes fresh UI.

Example state:
```
data class LoginState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null
)
```

Example intent:
```
sealed class LoginIntent {
    data class Submit(val username: String, val password: String) : LoginIntent()
    object ClearError : LoginIntent()
}
```

Example reducer:
```
fun reduce(state: LoginState, intent: LoginIntent): LoginState =
    when (intent) {
        is LoginIntent.Submit -> state.copy(loading = true)
        is LoginIntent.ClearError -> state.copy(error = null)
    }
```

The main advantage here is that this provides absolutely consisten UI. This is used for complext interactive screens, offline, replay, logging etc. The main disadvantage is larger boilerplate code, harder learning curve and large state objects. This is suitable for financial apps, messaging apps, multi step forms with complex UI. With Jetpack compose being state-driven, it naturally matches the MVI pattern. MVVM is good for 90% of the usecases. But there is a trend to move mode towards MVI with jetpack compose.

## UI Rendering Pipeline

1) Inflate XML → Views created in memory
2) Measure phase
3) Layout phase
4) Draw phase (record operations)
5) GPU renders to screen
6) Display refresh (usually 60Hz or 120Hz)

Inflation step converts XML to objects in memory. This is done by `LayoutInflator`. In this phase objects are created via reflection and its attributes are set. At the end of inflation a full hierarchy tree is created. This happens once per view creation and NOT pre frame.

For rendering UI, Android does measure, layout, draw. These are recursive DFS traversals of the view heirarchy tree. 

In the measure phase, we calculate teh width and height of each view. Parent will call `child.measure()`. View's `onMeasure` method is called. Typically this method is implemented by all android views. We need to override it only when we write a custom view. Based on the inputs and the constraints on the view such as wrap_content, match_parent, text size, image size etc, the `onMeasure` method proposes a size. As we can imagine for nested layouts this measurement runs into nested loops and they are costly.

Next phase in layout phase, where we calculate the (x, y) position of a view within its parent. Parent calls `child.layout()` and this results in `onLayout(changed, left, top, right, bottom)` API on the view. The child view is "informed" of its bounds. It stores them and uses them to render itself. It can also use these bounds and render its children. The API also gets a `changed` flag, which indicate if the layout was changed between the current and previous calls. If its not changed, the view can avoid any further expensive layout operations internally.

Next phase in draw phase, in which the views are drawn. Parent will call `child.draw()` and child's `onDraw(canvas)` gets invoked. In this canvas operations like rects, bitmaps, text, paths, gradients etc will get called. All view rendering boils down to such drawing methods. Android does not draw every pixel, it just records the commands and send it to the GPU via `RenderThread`.

On close observation these method pairs are template method pattern. `layout`, `onLayout` etc. The idea here is that `layout` is the public API to trigger and `onLayout` is a hook for the view to customize its behavior. 

One round of measure -> layout -> draw -> GPU submit is called a `frame`. Frames are produced only when something invalidates the view such as user interactions (touch, swipe) or animations or data updates. If there are no invalidations, there is no need to render frames. The refresh rate belongs to the Android screen. It can be 60fps or once in 16ms, 120fps or once in 8ms. So every interval when screen needs to refresh, the vsync component sends a callback the the current app in foreground to a component called `Choreographer`. This component is per UI responsible for sending the screen to be drawn on every refresh. It batches work between each frame and sends a final set of commands to the GPU via the `RenderThread`. 

## Janks
If UI takes more than the vsync time (16ms or 8ms etc. depending on the refresh rate of the screen) to go from the `invalidation signal -> measure -> layout -> draw -> RenderThread -> GPU` sequence, the UI does not appear smooth. This is called a jank. The main cases are:
- Main thread blockage
- Too many view traversals in measure -> layout -> draw phases.
- Running heavy operations such as DB or network call on UI thread.
- GC pauses

Android provides tools such as Android Studio Profiler, Layout Inspector, and FrameTimeline in Logcat to spot slow frames and fix them. Reducing the view heirarchy nesting should be considered strongly to fix janks, apart from fixing issues on main thread and figuring out any memory leaks reading to GC.


## Recycler view

_See [sample app](../sample-andoid-apps/ListingApp/)_ 

The main usecase that this solves is listing. Be it a list of messages, tweets, posts, images etc. Before recycler views, Android used `ListView` and `GridView` for listing purposes. These have limitations when it comes to layout flexibility, animations, reusing items is not clean to implement, there is no diffing mechanism etc. `RecyclerView` solves these short comings. It provides ability to render large lists efficiently, reuse the item views, supports animations and variable layouts, provides pluggable layout manager, support paging and infinte scroll.

The core architecture of `RecyclerView`:

```
RecyclerView
 ├─ LayoutManager
 ├─ Adapter
    ├─ ViewHolder
    ├─ DiffUtil
 ├─ RecycledViewPool
 └─ ItemAnimator
```

`LayoutManager` determines how items are arranged. There are many layout manager impementations available such as `LinerLayoutManager`, `GridLayoutManager`, `FlexboxLayoutManager`, `StaggeredGridLayoutManager` etc. (What does it take to write our own?)

`ViewHolder` holds references to item views. (what are item views and how are they created?)

`Adapter` creates and binds the `ViewHolder` to data

`RecycledViewPool` reuses views off-screen and avoids re-inflation

`ItemAnimator` handles animation during insert, delete or change.

Adding recycler view into the layout is like this:
```
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

Then we write the adapter. Adapter class extends `RecyclerView.Adapter` (different types of adapter?) and view holder extends from `RecyclerView.ViewHolder`.
```
class UserAdapter : RecyclerView.Adapter<UserAdapter.VH>() {
    class VH(val view: View) : RecyclerView.ViewHolder(view)

    // While creating view holder, internally we inflate the view and attach it.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return VH(view)
    }

    // when view holder is bound, we populate the view associated with the viewHolder
    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.view.username.text = user.name
    }

    // this provides the number of items
    override fun getItemCount() = users.size
}
```

Each item (R.layout.item_user) is just a normal layout file

```
<LinearLayout ...>
    <ImageView .../>
    <TextView .../>
    <Button .../>
</LinearLayout>
```

If there are multiple view types in a single recycler view, then view type has to be returned per position. (what do we return here? R.layout.*?)
```
override fun getItemViewType(position: Int): Int
```

When a list item scrolls off the screen, the view holder is put into the `RecycledViewPool`. It is reused for later items (now what happens when there are different item view types?). This avoid costly inflation. As a result, view creation does not happen during scroll. 

### Perf and best practices
Using `ListAdapter + DiffUtil` for better efficiency. DiffUtil calcualtes changes in O(n) time and it prevents `notifyDataSetChanged` (what and when is this called?), flickering and full rebinds. 

```
class UserAdapter : ListAdapter<User, VH>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.id == new.id
            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    }
}
```

Avoid using nested recycler views, use multiple view types instead. 

If the list size does not change based on content dimensions, then use `setHasFixedSize(true)`. This provides faster layout.

Use `getItemId` and return stable IDs from it. This helps in smooth animations and correct item movements

```
override fun getItemId(position: Int) = items[position].id
setHasStableIds(true)
```

### Pagination

If there is a fixed set of items use

```
    // this provides the number of items
    override fun getItemCount() = users.size
```

If there are paginated items and we load page one after the other do this:

For infinite scroll:

```
Pager ⟶ Flow<PagingData> ⟶ RecyclerView
 ├─ PagingSource ⟶ Room
 └─ RemoteMediator ⟶ Network client
```

RemoteMediator is invoked whenever there is a request for new pages (or prefech) that is not available in the DB. That does a network call and fetches data, stores into DB.
PagingSource provides a way to send DB updates as pages to the RecyclerView using Pager and PagingData. The remote updates thus reaches the UI.

### Preloading

If the user scrolls fast, there can be blank views and flicker. First we provide animated place holder views. Second we prefetch using `setItemViewCacheSize()`. This controls how many off-screen items are kepty in memory pre-bound for smooth scrolling experience.

```
recyclerView.layoutManager = LinearLayoutManager(context, ...)
layoutManager.isItemPrefetchEnabled = true

recyclerView.setItemViewCacheSize(10)

Pager(
  PagingConfig(pageSize = 20, prefetchDistance = 3)
)
```

### Recycler view during configuration change
`LayoutManager` restores scroll position automatically, but the data must be identical after recreation. If data changes in viewModel, then recycler view recreation can result in jumpy UI.

## UI level features

Traditionally to bind values to UI we use `find` methods such as `findViewById`. This is inefficient, cumbersome and error prone. Bindings were introduced to solve this issue.

### View binding
View binding is used for safe view lookups. It does not support binding expressions in XML to UI or two way bindings. This is faster and light weight compared to data binding. This is recommended for simple UI. This is used to replace `findViewById` calls. Enabled in gradle file using `viewBinding=true`

### Data binding
This is more complex than view bindings and has more capabilities. Enabling `dataBinding=true` in Gradle triggers the Data Binding compiler, which generates binding classes for your XML layouts, processes `@Bindable` fields, and wires two-way binding expressions (`@{ }`). It replaces `findViewById`, adds observable machinery, and integrates binding adapters. Essentially it turns your XML into a type-safe, generated API for accessing views and binding data.

### Image handling
_See [sample app](../sample-andoid-apps/ListingApp/)_ 

Image handling is critical in Android app as is heavy operation resulting from having to work with compressed formats such as JPEG, PNG, WEBP etc. Decoding these are CPU intesive opeartions. Images can cause UI janks, OOM errors, memory issues and even crashes in low end phones. 

Capturing image is easy now-a-days and the resoultion of images are quite big. A high definition image (1080 x 1920) can take approx 8MB (depending on format). So we need a smart way to handle images. We should:
- handle network download with correct caching headers, retry logic etc.
- decoding compressed formats to bitmap
- cache them to avoid expensive network calls (typically LRU cache)
- download correct size as required
- progressive loading (thumbnail + low res + high res)
- be lifecycle aware and avoid wasteful work with UI is dismissed.

Built in progressive loading will require server to stream images over http and image itself must be progressive. Fake progressive loading is done by loading low res image first and then high res image (based on if user is touching or expanding etc.) or just thumbnail + high res or low res + high res. These require backend to expose separate URLs for these images. This is typically supported by CDNs / image servers. Thumbnails can also be replaced by `BlurHash` strings which are light weight. This is generated on backend and android has support to render it on UI quickly using BlurHash decoder libraries.


An image library should support these functionalities. Some popular options are Glide, Fresco, Picasso, Coil etc.

In summary COIL (Coroutine ) is Google recommended as its kotlin first and coroutine friendly, small size and modern library. 
Main API is to use `load` extension function on an `imageView` to load the image. The configuration can be centralized and injected by DI to configure the in-memory and disk caching details.

## Attachments
Attachments are tricky to handle in mobile, since the files can be huge and cannot be loaded it fully into memory. Holding large files in memory can lead to OOM. Cellular and flaky network, switching between wifi and cellular, restrictions on what can run in background and possibility of the app being killed when not in foreground add to the complexities. Also, app has restricted access to the files stored in the mobile. Files come from variety of sources such as cloud providers (Google drive, one drive), 3rd party file pickers, download provider, storage etc. Thus its important that attachments are streamed, chunked and resumable.

```
User picks file → Get URI → Resolve metadata → Stream via InputStream → 
Downsample if needed → Upload (possibly chunked) → Notify backend → Cleanup temp files
```
When user picks a file, we get a `URI` and not a `File` in the code. That `URI` needs to be resolved to actual file path. Always use `contentResolver.openInputStream(uri)` to access the file. A stream always reads the file progressively and does not load the entire file into the memory.

In case of images or videos, there is an option to downsample the file which are heavy, thereby retaining good quality playable files, but at the same time reducing the size. There are libraries available which can help in downsampling images (Coil / Glade transformation) and video (ffmpeg-kit, used by apps like instagram and tiktok).

The upload should always be resumable, meaning, the file should be in app cache directory and in case of failure, the file is readily available with the app, rather than obtaining again from the content provider. 

Chunking is a common technique used, where a single file is split into multiple chunks of 256 kb, 512 kb or 1 mb. This is then stitched back together in the backend.  This can help to upload files without repetitive work over a flaky network, or even over app restarts. Backend should support chunking for this to work. There are standard protocols available such as Google Drive Resumable upload API, AWS S3 Multipart upload, Azure blob storage block upload etc. Typical workflow is:

1. Start upload session 

```
POST /uploads/start
returns uploadSessionId, chunkSize and URL
```

1. Upload chunk
```
POST /uploads/{uploadSessionId}
Headers:
    Chunk-Index: 3
    Content-Range: bytes 5000-5999/50000
Body: [chunk data]
```

1. Complete upload session
```
POST /uploads/{uploadSessionId}/finish
```

On top of this, each chunk can be gzip'ed as well. In this way, backend will have to decompress the chunks before stitching them together. There are two approaches in this. First is to compress each chunk separately. In this case, overall compression ratio is lower than compressing the entire file. Second is to compress the file first and then chunk it and send. The downside is that the upload is not resumable as gzip compression depends on all previous bytes. So when upload has to resume, the gzip has to start from the beginning of the file and not from the middle. Theoretically, if we wanted to resume, we could start gzip for entire file and start sending the chunks from where it dropped off last time. But this is a lot of wasteful work compared to gzip'ing each chunk. Another option is to first gzip the file and then send it as chunks. This works, but will incur extra storage (temporary, but could be significant), double read and IO (deal with original file and then gzip file), high upfont CPU cost (compressing 500MB video or even 20MB image album can take 5 to 10 seconds on mid end phones). The other problem is that many file formats are already compressed (like JPEG, PNG, MP4, PDF, HEIC etc). So we may spend unnecessary cost on those. 

For mobile usecase, its better to compress each chunk separately. HTTP header `Content-Encoding` should be set to `gzip` for each chunk when sending compressed content. The best strategy is to start uploading immediately, show a progress bar and do it in a reliable, chunked, resumable way. Even compressing chunks could be optional as it may just add overhead. Compressing makes sense only for large text or log files etc. For chat attachments, photos, videos, cloud store, messaging and social apps etc, compression can be skipped because users do not want processing delay.

Let's take an example of Google's resumable upload. It supports
- Chunking
- random byte ranges
- network interruptions
- resumeability at the exact byte offset
- no redundant uploads
- no need to keep all chunks in memory

Client sends a request to start upload 

```
POST https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable
Headers:
  Authorization: Bearer <token>
  X-Upload-Content-Length: <file-size>
  X-Upload-Content-Type: <mime>
Body:
  { "name": "myfile.jpg" }
```

Server responds

```
HTTP/1.1 200 OK
Location: https://www.googleapis.com/upload/drive/v3/files?upload_id=ABC123...
```

`upload_id` is a resumable upload session.

Google lets you choose any chunk size (e.g. 256 KB, 1 MB, 10 MB), but recommends >256 KB. Upload uses PUT with a `Content-Range` header:
```
PUT <upload-url>
Content-Length: 256000
Content-Range: bytes 0-255999/1000000

<binary data for the first chunk>
```

Success server response
```
308 Resume Incomplete
Range: bytes=0-255999
```

HTTP 308 means:
- The chunk was accepted
- Upload is not finished
- You may continue at the next byte offset

Suppose upload fails mid-transfer (network drop, app closed, etc.) then client asks 
```
PUT <upload-url>
Content-Length: 0
Content-Range: bytes */1000000
```

for which server sends

```
308 Resume Incomplete
Range: bytes=0-511999
```
which means “I have received bytes 0 through 511,999. Continue uploading from byte 512,000.” Then client can resume

```
PUT <upload-url>
Content-Range: bytes 512000-767999/1000000
```

When final chunk is sent client:
```
PUT <upload-url>
Content-Range: bytes 512000-999999/1000000
Content-Length: 488000
```

server responds

```
200 OK
{
  "id": "...",
  "name": "myfile.jpg",
  ...
}
```

### Media handling

ExoPlayer is the default choice when it comes to streaming media. Most apps like Youtube, Instagram, Spotify, Netflix, Teams etc use it. It gives good control over buffering, adaptive streaming, track selection, caching and [DRM](#drm-digital-rights-management). 

Client side should handle buffering, decoding, playback, caching, track switching, errors etc.

```
// Simple setup for ExoPlayer
val player = ExoPlayer.Builder(context).build()
val mediaItem = MediaItem.fromUri(url)
player.setMediaItem(mediaItem)
player.prepare()
player.play()

// Buffering control
LoadControl.Builder()
    .setBufferDurationsMs(
        minBufferMs = 15_000,
        maxBufferMs = 50_000,
        bufferForPlaybackMs = 2_000,
        bufferForPlaybackAfterRebufferMs = 3_000
    )
```

MediaPlayer (old tech) supports progressive streaming of MP4 files and HLS streaming. For details on streaming protocols, refer [this section](#streaming-protocols).

#### HTTP progressive streaming
Client does range requests (HTTP 206) and reads from byte offsets as needed. This works without any server intelligence, it just needs to support range requests. Most web servers support this. More [details here](#http-progressive-streaming).

```
GET /video.mp4
Range: bytes=100000-200000
```

#### HLS / DASH
For this backend should support video encoding into multiple qualities: 1080p, 720p, 480p etc. It needs to break the video into segments and provide a manifest. We will explore streaming backend technologies in [detail](#hls--dash).

## Android lifecycle and lifecycle components
Need for lifecycle - Android apps run on resource-constrained devices. They compete with other apps. They can be killed and relaunched several times. Lifecycle provides clear hook points for allocating resource at the right time, releasing them, setting up and tearing down UI etc. 

The entire app has a lifecycle which can be obtained from `ProcessLifecyleOwner.get().getLifecycle()`. It's ON_CREATE is dispached exactly once and ON_START, ON_RESUME events are dispatched as the first activity moves through these events. ON_STOP, ON_PAUSE as last activity moves through these events, with a delay (long enough to handle configuration changes cases). These are useful for tracking when app is coming to foreground and background.

Activity also goes through the lifecycle of create, start, resume (when app is ready to interact), pause, stop and destroy. When configuration change happens app goes through pause, stop and destroy and then immediately activity is launched which causes create, start and resume.

Fragment has two lifecycles. One is for fragment instance which goes through attach, create, destroy and detach. When the fragment object is created and attached to an activity `onAttach` and `onCreate` are called. Fragment can survive configuration changes if its retained by `FragmentManager`. A `FragmentManager` is associated with an Activity or a Fragment. It can have child managers if there are nested Fragments in Activities. It manages fragments, track which fragments exist, which are visible, execute fragment transactions, manage back stack, drive the fragment lifecycle. 

FragmentManager during config change, stores the fragment class names, arguments and save state bundles and restore them after activity is created and a new fragment manager is created. This is why Fragments need empty constructor and no assumptions or state apart from those saved in saved state bundles.

```
// Hypothetical and simplified
class FragmentManager {
  // contains all instances of fragments known to manager. They can be visible, hidden, detached, back stacked.
  val fragmentStore: Map<FragmentId, Fragment>
  // Subset, active fragments - only fragments attached to UI
  val addedFragments: List<Fragment>
  // A BackStackRecord is a frozen record of a bunch of operations commited via FragmentTransactions.
  val backstack: Stack<BackStackRecord>

  // Async, execute all transactions queued in next main-loop cycle.
  fun commit()

  // Execute immediatey all transactions queued in next main-loop cycle. Not recommended
  fun commitNow()

  // Forces all queued transactions to run immediately, not recommended.
  fun executePendingTransactions()
}

class FragmentTransaction {
  val fragment: Fragment
  val op: List<Op> // add, remove, show, hide, addToBackstack etc.
}
```

Fragment view however has createView, viewCreated, start, resume, pause, stop, destroyView. Fragments can be retained in backstack, but the view is not, so the view is created before its presented to the user and destroyed as soon as its not needed to conserve memory. Fragment binding should be created in `onCreateView` and cleared in `onDestroyView`

```
private var _binding: FragmentMyBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...) {
    _binding = FragmentMyBinding.inflate(inflater)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

Doing this instead is wrong

```
class MyFragment : Fragment() {
    lateinit var binding: FragmentMyBinding

    override fun onCreateView(...) {
        binding = FragmentMyBinding.inflate(inflater)
        return binding.root
    }
}
```

Fragments should use `viewLifecycleOwner` to observe flows or live data.

```
// Never do this. This will be tied to the Fragment lifecycle which will outlive the view and hence leak
viewModel.data.observe(this) { ... }

// Do this.
viewModel.data.observe(viewLifecycleOwner) { ... }
```

A view model is a lifecycle aware state holder which survives configuration changes and is destroyed when the scope owner is destroyed. View model can be obtained using `by viewModels()` in activity and fragments and by using `by activityViewModel()` when using shared view model in fragments.

```
Activity created
 → ViewModel created
 → Rotation
 → Activity recreated
 → Same ViewModel reused
 → Activity finished
 → ViewModel cleared (onCleared)
```

There are few lifecycle aware APIs that are important. 
- `repeatOnLifecycle`
- `launchWhenStarted`
- `viewLifecycleOwner.lifecycleScope`

```
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            render(state)
        }
    }
}
```
This is create a block which is repeated on every start event on the lifecycle owner's lifecycle.

## Intents
Intent is a message object which requests an action from another Android component. The component can be within an app or across apps. Intents can be used for starting activity, service or send a broadcast. Intent encapsulates action, data, type of data, flags, extras.

```
Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://example.com")
    putExtra("source", "notification")
}
```

There are explicit intents, which call out the exact class to launch. This is used within the same app and its safe. There are implicit intents which just tells what to do, but does not specify who should do it. It just provides the action and data + extras. These are resolved using intent filters. Intent filters are defined in the manifest, which tells each component what it can handle and what it expects.

```
<activity android:name=".ShareActivity">
    <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="text/plain"/>
    </intent-filter>
</activity>
```

Here the action is SEND, category is default, data is text. So when that matches in a intent, the said activity is a candidate. If there are more candidates system provides a chooser. Categories are DEFAULT, BROWSABLE, LAUNCHER, APP_BROWSER, HOME, OPENABLE etc. System addes DEFAULT if none specified.

Pending intent is a token that grants another app or system permission to perform an action on your apps's behalf. These are used with notitications, alarms, widgets etc. Pending intent can also do the same things as an intent.

```
val intent = Intent(this, DetailActivity::class.java)
val pendingIntent = PendingIntent.getActivity(
    this,
    0,
    intent,
    PendingIntent.FLAG_IMMUTABLE
)

notificationBuilder.setContentIntent(pendingIntent)
```

Pending intents run with your app's permission, even if your app is not running and can be fired by other apps. So there can be immutable and mutable pending intents. Immutable ones are more safe. Mutable ones allow the intent to be modified before firing (examples inline reply, bubbles etc.)

As a guideline, always use explicit intents for internal actions. Minimize implicit intents as other apps can intercept. 

Intent filters are used for deeplinking. 

```
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <data android:scheme="https" android:host="example.com"/>
</intent-filter>

This can allow your app to be opened using https://example.com/product/123
```

## Broadcasts and Receivers
Broadcast is a system-wide message sent as an intent. They are async and decoupled. It follows typical messaging paradigm within the Android mobile ecosystem.

A broadcast receiver is a component that listents for broadcasts and executes logic matching the broadcast. We can think of them as subscribers to the broadcast.

In general, use of custom broadcasts and receivers should be minimal as we use live data / flow etc. We can still use it for some important system level broadcasts if needed. Typically we still use it for BOOT_COMPLETED, SMS_RECEIVED, PACKAGE_UPDATED etc. For things like network connectivity, battery, app foreground / background etc, we have respective managers such as NetworkManager, BatteryManager, ProcessLifecycleOwner etc.

A typical example. Every receiver is instantiated and its `onReceive` method is called and then its destroyed.
```
System
  └── sendBroadcast(Intent(ACTION_BATTERY_LOW))
        ├── App A Receiver
        ├── App B Receiver
        └── App C Receiver
```

Recievers also use intent filters to narrow down the broadcasts which it wants to handle. So it matches the action, category and data. 

There are three types of broadcasts. First one is a system broadcast, which informs about battery condition, booting up etc. Second is the app boardcasts, which are sent by apps to other apps. Third is a local broadcast which is now deprecated and replaced by LiveData / Flow / event buses.

Broadcast receivers are of two types. First one is manifest driven, which can receive broadcasts when app is not running. This can be used to listen to system events like SMS_RECIEVED, BOOT_COMPLETED etc. What can be listened to in manifest are limited to system level and some key broadcasts. The second type is dynamic receiver which is registered in code. This is active only when app is alive and this is preferred in modern apps.
```
registerReceiver(receiver, IntentFilter(ACTION))
```

Receiver's lifecycle is very simple. It cannot do any long tasks, or async tasks. It can delegate to WorkManager or services.

`Create → onReceive() → Destroy`

The security issue with broadcasts is that other apps can imitate them. So its better to use explicity intents (implicit ones are easy to generate), receivers should be protected with permissions.

## Offline support
- Database - SQLite (other options?)
- Shared prefs
- Disk storage (files)
- Key store or secure storage

When to use what
Need details about schema, primary keys, relations, indices
How to encrypt DB? - Specific columns or entire DB?
Encryption methods - Key based - what key to use, how to encryt and decrypt - What algo to use etc.

## Deeper Android
Permissions management
Battery life 
Location
Camera
Bluetooth?
Network / wifi & 4G
Broadcast receivers

### Work managers

### Services - Bound and unbound

### APK structure
- Bundles / flavors etc

### Binder / IPC
How to build a separate telemetry / logging APK that can be reused as a service across multuple apps?

### Doze mode
Doze mode is an aggressive battery saving state introduced in Android 6. Doze mode progresses from Light to Deep. First when screen of off - no doze yet. Device is stationary -> light doze mode. Then after some time it enters deep doze where CPU sleeps most of the time, network is blocked, work deferred and only maintenance window work happens every 10 - 30 mins (increasing gaps). During this time Android wakes up breifly, runs pending jobs, delivers queued notifications, allows app to sync, re-enters doze mode.

Foreground apps, services, high priority push notifications FCM, System apps such as clock etc are only allowed. There is a maintenance window concept when background tasks are allowed in a short burst every few minutes. Alarms set using `setAndAllowWhileIdle()` kind of API are allowed.


## Security
How to login a user
How to store password
OAUTH and stuff

## Privacy
How to implement e2e encryption
How to encrypt data base, what to encrypt. 
What access levels are possible to storage such as secure, disk, db etc.

## Networking
Design of backend end points and data exchange should be clear.

### HTTP based REST
[HTTP explored in a later section entirely](#http). REST uses HTTP.

Although HTTP is action oriented (GET, PUT, POST etc), REST is resource oriented. `/users/{id}`, `/payments` etc. REST can work on a variety of payloads such as JSON, XML, YAML, HTML, protobuf etc. 

_REST over HTTP is stateless_. This means that every request should contain context such as auth token, pagination details, resourse identifiers. REST uses HTTP headers and error codes. REST does not support stream, push or bidrectional comms. 

_REST cannot provide granular data fetch_ like GraphQL, so most of the time you fetch more data than you actually need depending on the API design. So if you need to know if something changed or updated, you may end up calling the API and downloading the whole payload even if its unchanged. 

_This can be avoided by using techniques such as `eTag`_. `eTag` is simply a hashcode that is returned for a specific response. Client can then send this back in `If-None-Match` header. Now server may respond with `304 Not Modified` or with new data and a new `eTag`. This can be used for implementing optimistic concurrency for updates as well. Suppose `eTag: v2` was sent by server. Client can then say `PUT order/123, If-Match: v2`. If the version does not match, error `412 Precondition Failed` is recieved.

REST also provides `Cache-Control` header which specifies if the response can be cached by the client / CDN or cannot be cached. 

Idempotency is a key constraint in REST. Client may end up calling the same API multiple times in case of retry, network loss or failure recovery. In such cases, client should ensure that they do this only for idempotent cases. 

REST depends on HTTP semantics:

| HTTP Method | Purpose / Behavior| Idempotent? |
|-------------|-------------------|-------------|
| **GET** | Read-only | ✔ Safe + Idempotent |
| **PUT** | Replace entire resource | ✔ Idempotent |
| **DELETE** | Remove resource | ✔ Idempotent |
| **PATCH** | Partial update | ❌ Not necessarily idempotent |
| **POST** | Create / action / side-effects | ❌ Not idempotent|

_What does it mean to be PUT and DELETE to be idempotent?_ It is idempotent from a single client's perspective. Even if client calls PUT or DELETE on same resource with same payload multiple times, the state on server is exactly the same. But if there are two clients PUT ing same resource, the last one wins.

_POST is not idempotent, but there can be cases you have to retry_. How do we handle this situation? Say we have payment of 100 made. If we send this mutliple times, it will end up adding as many payments. This can be solved by using an `Idempotency-Key`. Idea is similar to `eTag` but this time, client owns the hash. 

_REST uses rate limiting and throttling headers_. They provide error code `429 Too Many Requests` and also `Retry After` header

### GraphQL
_GraphQL is a query language based backend that provides data in the format that the client wants_ using a single structured query. Instead of separate REST calls to fetch data, client can use a single end point and send a query on what data it wants.
```
GET /user/123
GET /user/123/friends
GET /user/123/photos
```
to
```
POST /graphql
{
  user(id: "123") {
    name
    friends {
      name
      avatar
    }
  }
}
```

_GraphQL works on top of HTTP_. It uses JSON payload for communication.

_GraphQL exposes a typed schema_. It supports querys for reading data, mutation for updating data, resolvers that are shortcuts to get to join data and subscriptions that are realtime updates over websocket.

At the server side, GraphQL parses the query that client sends, runs resolver functions per field and then returns the fields that client requested.

From Android standpoint, it simplifies the network stack as there is only one end point, fetches exactly what data is required, batches the fetch compared to REST end points, provides a strong typed result and provides offline capability OOTB using local persistence and optimistic updates.

To integrate GraphQL in Android app, integrate `apollographql` dependency, define `.graphql` query files. Apollo generates kotlin models after which use `ApolloClient` to connect to server and execute query, get the result. Internally Apollo handles caching, retries, OkHttp integration.

### Websocket
[Explored in real time comms.](#websocket-1)

### gRPC
[Explored in real time comms.](#grpc-1)

## Real time updates

|      Technique            |     Type / Notes          |
|---------------------------|---------------------------|
| FCM Push Notifications    | Server → device push using google services |
| Short Polling             | Repeated periodic HTTP    |
| Long Polling              | Long lived HTTP request, client initiated, keep alive and high read timeout |
| WebSockets                | Full-duplex persistent connection, HTTP initiated as upgrade, converted to 2 way binary stream on TCP, ping pong heartbeat. |
| Server-Sent Events (SSE)  | One-way server → client, client initiated, kept alive using heartbeat, client reconnects when dropped, client listens to updates. |
| gRPC Streaming            | HTTP/2 streams, bi-dir    |
| MQTT                      | Lightweight pub/sub       |
| GraphQL Subscriptions     | Real-time GraphQL events  |
| WorkerManager periodic    | Polling when app stopped  |
|    **Not explored** |       
| Socket.IO                 | WebSocket + fallbacks     |
| PubNub / Ably / Pusher    | Hosted real-time channels |
| Firebase Realtime DB      | Built-in real-time sync   |
| Firestore Listeners       | Built-in change streams   |


### Push Notification (FCM - Firebase cloud messaging)
When an Android app is stopped or killed, it cannot maintain background connections. In such cases, it needs to rely on push delivery mechanism for notifications. This happens through FCM which is Google's push service.

```
Your Server  →  Firebase Cloud Messaging (FCM)  →  Google Play Services  → Android System  →  Your App (if needed)
```

When user installs or opens the app, app calls `FirebaseMessaging.getToken()`. Google play service generates unique FCM token for that app+device. App sends this token to backend server. Token can change whenever user reinstall, clear data, update, or due to security rotation. App should update the backend whenever a new token arrives in `FirebaseMessagingService#onNewToken()` callback.

Backend can send notification through FCM using this token as a _normal or silent notification_, and have payload and priority attached to it. Server posts to FCM using HTTP v1 API with OAuth2 token provided by Google IAM, along with the app+device token. FCM then stores the message and determines the device, user, country, connection status, device doze mode, app restrictions etc. Most android devices maintain persistent connection with FCM. This is single connection per device for all apps. (XMPP over TCP). When device is online and criteria are met, a notification message is delivered. 

There are 2 types of notifications: Normal Notifications, Silent Notifications or data messages.

**Normal notifications** show up in the notification bar in Android System UI **without waking the app**. These notifications have fixed structure, and limited configuration. On tap it can wake up the app or launch the app or do some custom actions. Notifications can trigger custom actions by defining a `PendingIntent` to start service, boardcast reciever or activity. Some examples are play/pause button which sends broadcast to `MusicControlReciever` or `Mark as read` button which triggers a background service call without launching any UI. 

**Silent notification** are delivered only if:
- app is in foreground or 
- app in background and notification is high priority (if app is abusive and it sends many high priority messages, wakes up app and runs lots of background jobs, drains battery, its silently marked as abusive and high priority notifications are automatically degraded)  
- device is not in doze mode 
- or device is in doze mode and app is high priority (app being high priority is a heuristic based decision that Android does based on recent user interactions, foreground services, media usage, location usage, whitelisted background processes etc.). 
    
For silent notifications, android may temporarily wake the app in background. Silent notification is delivered to the app callback `FirebaseMessagingService#onMessageReceived`. App can use this time to update local DB, sync data or show custom UI. This is short wake time (~10 seconds) and no long running tasks allowed. Heavy work should be offloaded to a workmanager. App can build a custom notification with cool UI, expanders, custom actions etc and then post it also, so that user can see it as a normal notification.

When app is woken up in background, if it needs to do some sync or network calls, it is better to use work manager. Using background service is not allowed unless in very special cases. Work manager is scheduled and run when Android gives it a chance. So its the right way. App may be able to start a Foreground service. This can work only because app is _given a **temporary foreground** status when a high priority FCM data message arrives_. All work should be over in 10 seconds. Usage of foreground service should follow the guidance of showing foreground notification within 5 seconds of lauching the service. This option is given so that apps can do some realtime message sync, process VoIP calls, urgent health / safety alerts, download data needed for displayed notification. 

Delivery of notifications are not guaranteed. Its best effort. It may be delayed, dropped and availabilty of network at the device. FCM uses adaptive delivery. Notifications are throttled if too many silent notifications per hour, too much background work is triggered, user rarely opens the app, app is battery-heavy, device is on low battery or idle, device is in doze mode. Rough heuristics suggests that 10-20 silent notifications per hour is fine, 50 to 100 may trigger throttling, 200+ will surely result in severe throttling and blocking of background wakeups. Repeating silent notifications to wake the app is discouraged.

FCM server has rate limits per API call. For example `/v1/messages:send` has ~600 QPS per project (varies). Play store policy is the biggest deterent. Sending promotions or spam notifications, irrelevant, too frequent, deceptive pushes will result in suspendng the app and restricting the FCM usages.

#### Topic-based notification
Topic based notifications is a single push message to millions of devices at once. Topic can be something like "weather" or "score" or "global-alerts". Devices can subscribe to these topics and server can send one message to all subscribers. There is no token list, user segmentation or other complications. Topics are global so they can collide. In such case every subscriber recieves every message on the topic. Android app can subsribe thus:

`FirebaseMessaging.getInstance().subscribeToTopic("news")`

Topics are generic and anyone can subscribe to it. So it should never contain userId, email, private info or secrets. Topics can also support normal and data messages.

### HTTP Short polling
This is client specific implementation where client keeps polling server in some interval. A typical implementation is 2s or 5s. 
Server immediately responds, if there is an update or not. 
Server load will be high in this case, client battery drain is higher.

This can work with _legacy systems_ as it does not need any specific support from server side.
This can be used in situations when other realtime updates like notifications / websockets / long polling are not feasible.

### HTTP Long polling
In this model client makes a request, server may respond immediately if it has an update. Else it holds the connection and responds when a response is available or the request times out. Server load is less than short polling. But it is still significant as the connection has to be kept live.

Once the response arrives, client initiates another long poll request.

This works the same with HTTP/1 or HTTP/2 because the concept remains the same. No change in client or server side specific to HTTP protocol supported. With HTTP/2 we can have multiple long polling requests in a single HTTP/2 TCP connection.

#### How long does the server hold the connection?
This is mostly server implementation dependent. There is a timeout associated which client can configure. Typically this is anywhere from 15 to 60 seconds.

In case of Android, we can use network library which can set timeouts like `Read timeout` which can be increased to let the server know that client is willing to wait. HTTP header `keep-alive` should also be used. 


At the server side, there can be intermediate layers which support idle connections also like CDNs or API gateways. _Server has to implement the logic to wait until data is available instead of flushing the response prematurely_. Here server needs to implement waiting strategies involving non blocking IO and it should support sufficient number of concurrent connnections. Some stregies to hold connections is _event loop in Node, Async controllers in Java + Spring boot or similar async frameworks in other languages_.

### Websocket
Websockets provide full duplex (back and forth), persistent, low-latency connection between server and client. It works on messages rather than request / response. Its like upgrading HTTP to a pipe where both server and client can talk freely. 

Websockets starts off as an HTTP call. After server responds, they switch protocols. Once websocket is established, there is no more HTTP. 

First client sends an HTTP request with and `Upgrade` request header to the server, to indicate it wants to switch to websocket. It also passes websocket key and version. 
Server responds back with an accept `HTTP 101` code. After this comms are using websocket frames, using a bidrectional TCP connection.

Websocket communicates using _binary frames_. Websockets are long lived TCP connections. It is kept alive by a heart beat once in 30-60 seconds which server sends ping and client responds with a pong.

Websocket initialization cannot happens using HTTP/2 as it does not support `Connection: Upgrade` functionality which is the core of establishing websocket connection. 

As with HTTP/3 there is a new protocol called `WebTransport` whch is designed for this usecase.

In Android, all popular network libraries like OkHttp, Ktor, WebView etc support websocket.

As for server side, it should also support websocket connections and implement the communication specific to the functionality. Server should maintain connection state, decode/encode frames, handle concurrency, broadcast messages, manage connection lifecycle.

### Server sent events
These are unidirectional, lightweight, simple way for server to push events/messages to the client over HTTP (not using WebSockets). Typically used for Live score, stock updates, notifications, chat message updates, streaming logs etc. 

Client opens a normal HTTP request, server keeps the connection open forever, server sends events as plain text, client recieves and updates automatically.

```
GET /events HTTP/1.1
Accept: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

Server sends periodic heartbeat messages which are ignored by the client. If client is disconnected, it automatically reconnects. 

SSE works well with HTTP/2 because HTTP/2 is by default multiplexed. So SSE streams are cheaper as it does not need additional connections.

Unlike browsers which have built-in support for SSE, Android apps do not. They need to use an OkHttp connection and keep reading the incoming messages continuously. 

Similarly the automatic reconnection does not work in Android. Its supported by browsers. In Android when connection breaks, we should implement retry, reconnect logic and include last-event-id in the header.

Although SSE transmits plain text, when using HTTPS its encrypted by TCP/TLS layers. SSE is nothing but an HTTP call.

### gRPC
gRPC stands for google RPC. It is a high perf, strongly typed, binary RPC framework created by Google. It is designed for microservices, mobile to backend comms, low-latency real-time systems.

gRPC uses protocol buffers (protobuf) to define API schema, serializing data to compact binary and generating strongly typed client/server code. The concept is similar to old style RPC where marshalling and unmarshalling is done across client and server, but client feels like they are invoking a local function.

gRPC suppots 4 modes of communiacation. 
- Unary → single request response model
- Server streaming → where client sends one request and sever streams multiple responses. Useful for live status, log stream, continuous results, info notifications.
- Client streaming → Client sends a stream and server replies once. Used for file upload, batch upload logs etc. 
- Bidirectional streaming → Both sides send streams independently. Useful for chat, live multiplayer gane, real-time collab etc.

Android uses Java/Kotlin gRPC library. Define `.proto` file, add gradle plugins. This generates stubs for RPC methods and then use this for making calls. 

Since gRPC uses HTTP/2 internally, it is encrypted (TLS), multiplexed, binary efficient protocol (faster than JSON REST), provides strong typing, presistent connections and bidirectional streaming support all in one.

gRPC over HTTP/3 is evolving, but not yet common in Android.

```
gRPC (API layer)
↓
HTTP/2 (application)
↓
TLS (presentation & session = encryption)
↓
TCP (transport)
```

gRPC needs a `.proto` schema which defines data structures, field number and services. This is a fixed schema which should be identical between client and server as the marshalling and unmarshalling will depend on this. gRPC is forward and backward compatible, it will ignore unknown fields and set defaults to optional fields.

```
syntax = "proto3";

message User {
  int32 id = 1;
  string name = 2;
}

service UserService {
  rpc GetUser(GetUserRequest) returns (User);
}
```

gRPC recommends to design the schema so that there are no removals or fields or reuse of field numbers or changing the meaning or type of the field. We can however add new fields, change field name (keeping the field number same), change the defaul values, adding new RPC methods or even deprecating fields. It is also recommended not to have required fields.


### MQTT
Message Queing Telemetry Transport. This is a lightweight, pub-sub messaging protocol built on top of TCP, designed for real time communication, especially over unrealiable and low-bandwidth networks. This is widely used on IOT devices, sensors, chat apps and mobile apps.  In this the client connects to a central broker and publish or subscribe to named topics for real-time message delivery. MQTT scales to millions of connections 

MQTT works well on low power devices, unstable networks, intermittent connectivity, minimal bandwidth. MQTT packets are as small as 2 bytes. There are no headers, no additonal TLS, no cookies, no req / resp. Its just one long TCP session. Only limited packet types and minimal fields. MQTT broker maintains the client id, subscription list, QoS state and pending offline messages. The client can reconnect instantly after a drop. It scales horizontally by adding more brokers and each broker handling thousands of TCP connections, highly optimized IO, fan-out optimization etc. Other standard geo distributes clustering strategies can be used.

MQTT has its own protocol written on TCP, and it has a broker which recieves messages, routes them to subscribers and maintains client session. It also provides QoS of:
- 0 → at most once
- 1 → at least once or 
- 2 → exactly once (most costly) guarantees.

#### How to use MQTT in an Android chat app?
MQTT works well when app is in foreground. But must have fallback mechanism when app goes to background.

Initially app connects to MQTT broker and subscribes to topics such as `chat/<userid>/messages` and `chat/<chatid>/events`. Publishes outgoing messages to `chat/<chatid>/send` This works great.

When app goes to background, TCP connection is killed during doze mode. App cannot keep MQTT running, broker will see client disconnect. messages will queue up. So MQTT should pair up with FCM to push silent notifications to periodically connect back to the broker and sync messages (and post a custom notification). There is an interesting concept called `Last will message`. This is registed by the client on the broker when it connects. When broker finds a client has dropped, it publishes that `Last will message` to some topic like `presence/<userid>/status`. Backend can subscribe to this and determine when to send the FCM push.

#### How MQTT handles large files and attachments?

MQTT message are kept small. Mostly < 64kb. They do not have large files or images, instead they contain metadata. App users HTTPS to get to the attachment and files when needed.

```
User uploads media → Backend stores in S3/CDN → MQTT publishes message metadata → Receiver gets MQTT → App fetches media via HTTPS
```

### MQTT uses TCP, is it secure?
MQTT implements its own TLS, so end to end communication is encrypted. MQTT supports TLS based auth using certificates as [explained in TLS 1.3 in HTTP section](#about-tls). It also supports mutual TLS where client can also send a certificate which is verified by the broker. Second way is using username and password. Password can be text which is discouraged, can be JWT token or OAith token that is short lived. It can also do token based auth. Once auth'ed broker creates a clientid and creates a persistent session by using a heartbeat. For network drops, MQTT does a TCP fast reconnect by using minimum formalities.

### GraphQL subscriptions
[See GraphQL section](#graphql) for basic details.
A sample subscription for a chat whenever a message is added to chatId 123 is as follows:

```
subscription {
  messageAdded(chatId: "123") {
    id
    sender {
      name
    }
    text
  }
}
```

Android client simply subscribes to the subscription object generated from the query json and collect on the flow. 
GraphQL uses WebSocket or SSE for subscriptions. It uses one websocket per app. This allows bidirectional messages. Client can start and stop suscription. Server pushes whenever there is an update.

The limitation here is that this works only when app is in foreground.  

### BLE Notifications

BLE stands for Bluetooth Low Energy. This are really low cost way to push events from a BLE device. Android app can connect to BLE device and subscribe to a characteristic. BLE pushes a notification when its value changes. App recieves `onCharacteristicChanged()`.

If one wants to access BLE device from their app, they should start off with asking for permissions to scan and connect bluetooth and location. With this app can use `BluetoothAdapter` and related API along with `LocationManager` to scan for BLE devices. Once device is idenfied use `connectGatt` to connect and then impelment a callback `BluetoothGattCallback` to recieve callbacks on connection, service discovery, read,write and notification events.

### Workmanager periodic workers


### Summary of network stack
| WebSocket | SSE | GraphQL  | gRPC | MQTT |
|-|-|-|-|-|
| HTTP/1 only for upgrade,<br> TLS + TCP <br> No HTTP/2 (No upgrade) <br> No HTTP/3 (has WebTransport)  | HTTP/1/2 | HTTP 1/2/3 <br> WebSocket or SSE for subscription | HTTP/2 only | TCP - Implements own TLS |

## Streaming protocols
Streaming is a very important aspect of mobile apps as there are several use cases to play videos, which are high quality and large, over variable network quality, instant start up, multiple qualities (240p to 1080p), with features such as audio + video + subtitles, seek, live stream, [DRM](#drm-digital-rights-management) / encryption and offlining capabilities.

Basic option is MP4 over HTTP and that solves some of these problems. But there are more streaming focused protocols available which are highly efficient and powerful to cater to all these requirements.

Let us understand the core building blocks of streaming.

1. Segmentation - Media split into small chunks, 2 - 10 seconds typically. Each chunk is independently decodable. 
1. Manifest - Details about available qualities, segment URLs, timing information, audio and subtitle tracks
1. Adaptive Bitrate - Ability of the client to switch quality based on bandwidth, buffer health and CPU
1. Stateless - Segments are sent over plain HTTP, which makes it CDN friendly, cacheable and scalable.

Now let's look at various Streaming protocols:

### Progressive download
This is a single MP4 file with HTTP range requests. 
```
GET video.mp4
Range: bytes=100000-200000
```
This is an extremely simple setup which does not require any special server setup. Its supported by web servers over HTTP.
This does not provide adaptive bitrate, no live streaming, no track switching and poor seeking for large files.
From a mobile perspective, it is good for short videos (feeds, reel) or for simple playback. It does not scale for larger videos and unstable network conditions.

### HLS (HTTP Live Streaming) - Apple standard
This is a segment-based adaptive streaming. It uses .m3u8 as a manifest. 
```
master.m3u8
 ├─ 1080p.m3u8 → seg1.ts, seg2.ts...
 ├─ 720p.m3u8
 └─ 480p.m3u8
```
This is good for mobile use cases in general. It works over HTTP, supports live streaming, CDN and DRM.
The downsides of this are higher latency (improved with LL-HLS) and has slightly more overhead compared to DASH
Not ideal for live events.

#### LL-HLS (Low latency HLS)
Reduced latency, used by Apple, Youtube Live, sports apps.

### MPEG-DASH (Dynamic Adaptive Streaming over HTTP)
This is based on open standard and has better codec flexibility, efficient ABR and strong DRM support. It uses `mpd` manifest file
The downside is that safari does not support it natively. So its an issue with iOS Safari without player apps. It works well for Android and ExpoPlayer.
```
manifest.mpd
 ├─ video 1080p segments
 ├─ video 720p segments
 ├─ audio tracks
 └─ subtitles
```
Not ideal for live events.

#### LL-DASH (Low latency DASH)
Similar goal as LL-HLS. Supported by ExpoPlayer.

### WebRTC (Real Time Streaming)
This is real time peer-to-peer streaming using UDP over a persistent connection. It has ultra low latency and two way communication. 
This is not CDN friendly and involves complex signaling and poor scalabilty. This is good for video calls and live interactions. It does not support ABR. WebRTC uses RTP under the hoods.

#### RTP (Realtime Transfer Protocol)
This is a low-level protocol aimed at solving realtime streaming. This uses UDP and thus does not provide any realibility standards. This works by transferring raw media frames or compressed packets (does not work with files or segments). Its ideal for VoIP, video calls, conferencing. 

### RTMP (Real-Time Messaging Protocol)
This is a powerful low latency live streaming protocol which runs over a persistent TCP connection. It sends audio / video, control messages and metadata. This was historically used in Flash players. It does not support CDN or ABR. Mobile do not have support for this playback. 

Now this is not used in playback anymore. But its still used in ingestion.

```
Camera / OBS / Mobile App
   ↓ RTMP
Ingest Server
   ↓ Transcode
HLS / DASH
   ↓
CDN
   ↓
Mobile / Web players
```

Many platforms such as Youtube Live, Facebook live, Twitch, Instagram Live etc use this for sending the camera feed over to the server. Futher for playback it uses HLS / DASH

## DRM (Digital Rights Management)
DRM stands to protect premium media content from unauthorized copying, screen recording, downloading etc. Media with DRM is encrypted. Only authorized apps can decrypt it. 

```
Encrypted media (segments)
        ↓
   License server
        ↓
 Decryption keys
        ↓
 Trusted device hardware
```

Common DRM systems:
```
| Platform | DRM |
|-|-|
| Android        | Widevine  |
| iOS / Safari   | FairPlay  |
| Edge / Windows | PlayReady |
```

Common workflow:
```
User presses play
   ↓
App requests media manifest (HLS/DASH)
   ↓
Manifest references encrypted segments
   ↓
Player detects DRM info
   ↓
Player requests license
   ↓
License server validates user & device
   ↓
License (keys) returned
   ↓
Player decrypts segments
   ↓
Playback starts
```

The license server decides who can play what. License request needs device information, user auth token, DRM challenge, content ID. License response includes encrypted keys and playback rules (expiry, offline allowed etc.) The keys sent are encrypted using public key that is generated per device and is provisined once during the device manufacturing, stored in TEE (Trust Execution Environment) which is special secured hardware on the device (which runs separate from Android with its own mini OS). 

Offline playback is supported where app downloads encrypted segments. Segments are always encrypted at rest. App requests for an offline license and the key is stored in secure store. Playback works until the key expires. 

## HTTP

HTTP is hyper text transfer protocol. It is an application layer protocol used by the web. 
HTTP/1 is textual protocol, HTTP/2,3 is binary protocol. It is stateless and request response based.
HTTP is not directly responsible for reliability, ordering, encryption etc. It uses underlying TCP/TLS or QUIC over UDP to achieve this. 
HTTP is a language spoken between apps. It defines a request and response format. 

Request format:
```
GET /profile HTTP/1.1
Host: example.com
User-Agent: Chrome
Accept: application/json
```
Response format:
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42
{"name": "Anand", "role": "Android dev"}
```
HTTP supports several methods: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS etc
HTTP has several statys codes: 
- 1xx → informational
- 2xx → success variants
- 3xx → redirection 
- 4xx → client error 
- 5xx → server error

```
HTTP/2 (application)
↓
TLS (presentation / encryption)
↓
TCP (transport)
↓
IP (network)
```

### HTTP/1.x vs HTTP/2?
HTTP/2 supports multiplexing. Which means that in single TCP connection, we can send multiple requests. Results in lesser and faster shake hands. Requests do not block on each other (no HoL at HTTP level). 

HTTP/2 provided support for `Server push`, which is now deprecated. This allowed server to push to client without request. But this had inherent issues over resource consumption.

For Android, OKHttp is multiplexing enabled by default. This can be disabled explicitly by setting protocols while creating the client to use only HTTP_1_1

Every HTTP/1 connection needs to open TCP connection. Browsers work with 6 to 10 TCP connections in parallel, OKHttp has 5 as default. But more TCP connections results in more handshakes, more memory consumption. 

HTTP/2 connection works on single TCP connection and then multiplexing inside that. HTTP/2 does this by switching to binary format from text in HTTP/1. Internally it splits comms into streams, each stream contains messages, and each message contains frames. TCP does not know about this. HTTP/2 does this at both ends. So if there is a packet lost, HoL kicks in for TCP and all streams are affected. 

HTTP/2 uses ALPN to avoid extra round trip to determine the protocol for communication.

HTTP/3 is coming up with QUIC protocol (which itself runs on UDP) which removes TCP level HoL also. 

#### Head of Line blocking (HoL)
A previous slower request holds a following quick request at ransom. The phenomenon where requests wait is called "Head of Line" blocking. TCP delivers a byte stream. If some packet is lost, then recieving side must wait. But later packets already arrived, they are stuck behind the missing packet and the flow proceeds.

For HTTP/2 HoL is a non issue in HTTP layer. In TCP layer HoL still happens. This is significant advantage for HTTP/2 because TCP layer HoL happens only when packets are lost. 

### About ALPN
Application Layer Protocol Negotiation. This helps in agreeing upon which protocol to use (like HTTP/1.1, HTTP/2, gRPC etc.)

Without ALPN, client sends a spcial upgrade header in first request and from that server understands. This needs extra round trip. It could not work with encrypted HTTP/2 as server could not peek inside TLS.

In ALPN, in the initial request itself client sends a list of protocols that it can support ["http/1.1", "h2"] and server picks one and reponds when it sends the public key and it is settled.

### About TCP
TCP is a low level network protocol for communication. It starts with 3 way handshake.

```
SYN (c to s)→ SYN-ACK (s to c) → ACK (c to s)
```

This ensures that the both sides can send and recieve. TCP is stateful, reliable, ordered and deterministic connection. Both client and server maintains state. The packets are transmitted in a certain order and assembled in same order at reciever side. Missing packets are re-transmitted and uses TCP HoL blocking while missing packet arrives.

Reliability and retransmission happens based on the ACK signal that client sends. If server did not recieve the ACK with the seq num, then it sends the packet again.

Used by all HTTP, TLS, WebSocket, Git, SSH etc.

TLS is an encryption layer on top of TCP. The stack is HTTP -> TLS -> TCP -> IP

### About UDP
UDP is a stateless, fire and forget kind of connection. There is no inherent reliability or order guarantees. This means UDP can be much faster, and useful where packet loss is not a major concern. Useful in WebRTC, Video streaming, VoIP, Games, DNS lookup etc.

### About TLS
Transport Layer Security gives HTTPS its 3 guarantees: 
- Encryption (attacker cannot read data)
- Integrity (attacker cannot modify data)
- Auth (communication is from a real server and not a fake one). 

TLS runs on top of TCP.

#### TCP + TLS 1.3 handshakes
TCP needs 3 way handshake 

```
SYN (c to s) → SYN-ACK (s to c) → ACK (c to s)
```

TLS does this in addition:

Client starts comms by sending supported TLS version, protocol suites, client random number, ALPN (HTTP protocol negotiation)
Server sends TLS certificate chain, public key, protocol selection, server random number. The public key is encrypted using the servers long term public key (which is typically client pinned or root CA)
Client has to decide if certificate is trustworthy using client validation. It does so using domain match, expiry, chain or trust (server cert -> intermediate cert -> root cert). Root cert must already exist in client's trust store. It also checks the certificate signature. If certificate is modified, then signature becomes invalid. 

Android uses system trust store (OEM managed) or app-added certs to store the root cert.

After cert validation, client generates a pre-master secret (random number). Client encrypts with server's public key (asymmetric key) and sends. Only server can decrypt as it has private key. Now both sides have server random, client random, pre-master secret. Using these values, both derive a session key. Both exchange readyness that they are now encrypted. From this point onwards all communication is encrypted using the pre-master secret (symmetric key).

```
Client Hello  ——>  (supported ciphers, client random, SNI)
              <——  Server Hello (server random, chosen protocol)
              <——  Certificate (server public key)
              <——  Certificate Verify
Key Exchange (client encrypts pre-master with server public key)
             <——/——>  Derive symmetric session keys
Client Finished ——> (encrypted)
Server Finished <—— (encrypted)
             <——/——>  Encrypted HTTP payload

```             

This can be further summarized as:

```
ClientHello -> ServerHello -> Secret sharing -> Finished encryption.
```

#### Prefect forward secrecy (PFS)
This is supported in TLS 1.3 which generates a private key at the server per session. This way old private keys, if leaked somehow, cannot be used to decrypt old encrypted comms.

There are 2 set of keys. One is a long term key and one is ephimeral pair to exchange secrets. The ephimeral one is generated every handshake. So what actually happens is that server sends the long term public key and an ephimeral public key which is encrypted with the long term public key. So that client knows its server who is sending this ephimeral public key. These session keys are the ones destroyed after each session. The ephimeral key only serves to exchange secret from client to server. After that all comms use symmetric keys (secret)

#### Symmetric and Asymmetric keys
Asymmetric keys are public, private pairs. Private key is the key that can open the mailbox and public key is like the location of the mailbox. This method is slow, but security is very strong. This is typically used for exchanging keys or verifying identities. Clients use this to encrypt their pre-master secret (which is a symmetric key). 

A symmetric key is when both sides use same secret key. Whoever has key can encrypt as well as decrypt. This is very fast and has strong securty (AES-128, AES-256) and is used for bluk data encryption.

### About QUIC
QUIC protocol is used with HTTP/3. Its a transport layer developed by Google. Its a modern replacement for TCP + TLS, built on top of UDP. It provides key features like multiplexing, congestion control, encryption, faster handshake, anti-HoL blocking, connection mitigation.

QUIC works on a connection ID and does not depend on IP address, port or network interface. This way switching network from wifi to 4G does not affect QUIC communication.

HTTP/3 has semantics over QUIC instead of TCP. This is supported in modern browsers and CDNs. For Android support is experimental in OkHttp. Supported for gRPC over HTTP/3. Apps like Search, Gmail, Youtube uses QUIC.

#### How QUIC do TLS 1.3 in 1 RTT?
QUIC integrates [TLS 1.3 inside its handshake](#tcp--tls-13-handshakes), exact 3-4 steps, but done in 1 round trip time (RTT)! Faster 0-1 RTT handshake as compared to 2 RTT for TCP.

The reason is that in case of TCP, its own handshake takes 1 RTT, at which point TLS is blocked. Client cannot send ClientHello unless the TCP handshake is done. But in case of QUIC, TCP handshake is avoided.

Client -> QUIC Initial (contains ClientHello)
       <- QUIC Initial + Handshake (contains ServerHello) **1 RTT**
Client -> Finished + app data

In the case of resumed connections, its 0 RTT because when client already has established a server session and is holding the session details, it can simply reuse it and send the data in the first request when it resumes. 0-RTT is not secure, it can be replayed by attackers. Most secure systems disallow 0-RTT.

#### How does QUIC work for HTTP which is reliable and order guaranteed protocol?
QUIC uses UDP which does not have any guarantees on delivery or order. QUIC implements ordering, reliable delivery, congestion control etc itself without the constraints of TCP. For example QUIC retransmits frames and not entire packets which are more efficient. HoL is avoided because QUIC support multiplexing and when data in one stream is lost, only that stream waits. Other communication continues. So, it narrows down the block to specific communication stream than blocking everything.

#### So why we can't get TCP fixed? 
TCP lives in the kernel and changing that takes years. QUIC is in user space and does not need kernel changes. Means QUIC is implemented in the application layer and not in kernel layer. It just uses UDP functionality from the kernel.

### HTTP Layering
| LAYER / STACK | HTTP/1.1 | HTTP/2 | HTTP/3 |
|-|-|-|-|
| Layer 7 Application | REST, JSON APIs, GraphQL, HTML | REST, gRPC (requires HTTP/2), GraphQL, HTML | REST, GraphQL |
| Layer 6 Presentation | TLS (HTTPS), separate from TCP  | TLS (HTTPS), separate from TCP  | TLS inside QUIC (NOT separate) |
| Layer 5 Session | HTTP/1.1 connection + keepalive | HTTP/2 Streams (multiplexing) | QUIC Streams, Multiplexing |
| Layer 4 Transport | TCP | TCP | UDP |
| Layer 3 Network | IP (IPv4 / IPv6) |
| Layer 2 Data Link | Wi-Fi, Ethernet, LTE/5G, etc. |
| Layer 1 Physical | Radio waves, fiber, copper, etc. |


### Root certificates
Provided OOTB by Google in Android devices. Derived from recognized gloabl certificate authorities. Audited and approved by Google. This is basis of HTTPS trust on Android. These certs cannot be modified without root access. Root CA updates happen though occassional Google updates, which adds new trusted CAs, removes compromized ones and updates metadata.
Location: `/system/etc/security/cacerts_google`

#### Certificate PINNING in Android
If an app wants to add additional trust CAs, then they can PIN certificates or trust only specific CAs, add user CAs or ignore system CAs totally.

Use can also install extra root CAs in location `/data/misc/keychain/certs-added`. These are not trusted by default apps. They are trusted only if apps opt in.

Normally HTTPs trusts all root CAs. But if an app wants, it can pin certain CAs which it wants and only those certs are trusted for that app's HTTPS comms. So even if there is a mallicious root CA that user installed or wifi provider inserts a proxy or a global CA is compromized, the app wont be affected. Pinning is simply overriding default trust CAs for a particular app (mainly to narrow it down).

Pinning public key is more popular as it allows cert rotation without breaking pin. App can also pin multiple keys for fallback. (say current key and next key, before removing the current one, so that both will work for some time.)

Pinning can be done in an app via XML `<network-security-config>` setting where you put in the public key as base 64 encoded string OR in code by using OKHttpClient's `CertificatePinner` while setting up a client.

During TLS handshake, after normal certificate validation succeeds, the app additionally checks the certificate or public key against its pins. If they don’t match, the connection fails. Pinning protects against MITM attacks, compromised CAs, and user-installed root certificates, but requires careful key rotation planning.

### 7 layer network model
| Layer| Description |
|-|-|
| Layer 7 Application | HTTP (REST, gRPC, WebSockets) |
| Layer 6 Presentation | TLS/SSL (encryption) |
| Layer 5 Session | TLS session, sockets |
| Layer 4 Transport | TCP or UDP (for QUIC/HTTP/3) |
| Layer 3 Network | IP (IPv4/IPv6) |
| Layer 2 Data Link | Wi-Fi, Ethernet, LTE/5G |
| Layer 1 Physical | Radio waves, fiber optics, wires |

## XMPP


## Backend
Load balancer
CDN
server capacity 
DDoS attack 
Exponential Backoff
API Rate-Limiting
