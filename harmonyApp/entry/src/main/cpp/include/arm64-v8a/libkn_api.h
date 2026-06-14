#ifndef KONAN_LIBKN_H
#define KONAN_LIBKN_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libkn_KBoolean;
#else
typedef _Bool           libkn_KBoolean;
#endif
typedef unsigned short     libkn_KChar;
typedef signed char        libkn_KByte;
typedef short              libkn_KShort;
typedef int                libkn_KInt;
typedef long long          libkn_KLong;
typedef unsigned char      libkn_KUByte;
typedef unsigned short     libkn_KUShort;
typedef unsigned int       libkn_KUInt;
typedef unsigned long long libkn_KULong;
typedef float              libkn_KFloat;
typedef double             libkn_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libkn_KVector128;
typedef void*              libkn_KNativePtr;
struct libkn_KType;
typedef struct libkn_KType libkn_KType;

typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Byte;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Short;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Int;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Long;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Float;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Double;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Char;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Boolean;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Unit;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UByte;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UShort;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_UInt;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_ULong;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Function1;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_runtime_Navigator;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_Platform;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_router_generated_GlobalRouteAggregator;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_collections_List;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_collections_Map;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_router_generated_RouterRegistry_ComposeApp;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_router_generated_ServiceRegistry_ComposeApp;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_router_RouterContext;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_RotateScaleTransitionStrategy;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_animation_AnimatedContentTransitionScope;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_runtime_StackEntry;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_animation_EnterTransition;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_androidx_compose_animation_ExitTransition;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserDetailData;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Any;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserDetailPreloader;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_CounterViewModel;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_coroutines_flow_StateFlow;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_DynamicFeatureGuard;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserProfile;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserDetailArgs;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_serialization_descriptors_SerialDescriptor;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlin_Array;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_serialization_encoding_Decoder;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_serialization_encoding_Encoder;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_screens_UserDetailArgs_Companion;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_kotlinx_serialization_KSerializer;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_services_AnalyticsServiceImpl;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_services_UserService;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_services_UserServiceImpl;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_Greeting;
typedef struct {
  libkn_KNativePtr pinned;
} libkn_kref_shijing_tianqu_OhosPlatform;

extern void* MainArkUIViewController(void* env);
extern void InitJsRenderNodeContext(void* env, void* nodeConstructor, void* statusModifyConstructor, libkn_KDouble ratio, libkn_KBoolean fixed);
extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToAppear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToDisappear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_cancelSyncRefresh(void* controllerRef, libkn_KInt refreshId);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchHoverEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchMouseEvent(void* controllerRef);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchTouchEvent(void* controllerRef, void* nativeTouchEvent, libkn_KBoolean ignoreInteropView);
extern void AndroidxComposeUiArkuiArkUiViewControllerDraw(void* controllerRef, void* canvas);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_findNodeIdAt(void* controllerRef, libkn_KFloat x, libkn_KFloat y);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_getId(void* controllerRef);
extern void* AndroidxComposeUiArkuiArkUiViewControllerGetJsNode(void* controllerRef);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_getRendererTypeId();
extern void* androidx_compose_ui_arkui_ArkUIViewController_getXComponentRender(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_initFusionRendererNode(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillShow(void* controllerRef, libkn_KFloat keyboardHeight);
extern libkn_KBoolean androidx_compose_ui_arkui_ArkUIViewController_onBackPress(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onConfigurationUpdate(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFinalize(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFocusEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFrame(void* controllerRef, libkn_KLong timestamp, libkn_KLong targetTimestamp);
extern void AndroidXComposeUIArkUIArkUIViewControllerOnIdle(void* controllerRef, libkn_KLong timeLeft);
extern void androidx_compose_ui_arkui_ArkUIViewController_onKeyEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageShow(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceChanged(void* controllerRef, libkn_KInt width, libkn_KInt height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceCreated(void* controllerRef, void* xcomponentPtr, libkn_KInt width, libkn_KInt height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceDestroyed(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceShow(void* controllerRef);
extern libkn_KInt androidx_compose_ui_arkui_ArkUIViewController_requestSyncRefresh(void* controllerRef);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_sendMessage(void* controllerRef, const char* type, const char* message);
extern void androidx_compose_ui_arkui_ArkUIViewController_setContext(void* controllerRef, void* context);
extern void androidx_compose_ui_arkui_ArkUIViewController_setEnv(void* controllerRef, void* env);
extern void androidx_compose_ui_arkui_ArkUIViewController_setId(void* controllerRef, const char* id);
extern void androidx_compose_ui_arkui_ArkUIViewController_setLocaleAndStringProvider(void* controllerRef, void* provider);
extern void androidx_compose_ui_arkui_ArkUIViewController_setMessenger(void* controllerRef, void* messenger);
extern void androidx_compose_ui_arkui_ArkUIViewController_setRendererBackendId(void* controllerRef, libkn_KInt backendId);
extern void androidx_compose_ui_arkui_ArkUIViewController_setRootView(void* controllerRef, void* backRootView, void* foreRootView, void* touchableRootView);
extern void androidx_compose_ui_arkui_ArkUIViewController_setUIContext(void* controllerRef, void* uiContext);
extern void androidx_compose_ui_arkui_ArkUIViewController_setXComponentRender(void* controllerRef, void* render);
extern void androidx_compose_ui_arkui_init(void* env, void* exports);

typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libkn_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libkn_KBoolean (*IsInstance)(libkn_KNativePtr ref, const libkn_KType* type);
  libkn_kref_kotlin_Byte (*createNullableByte)(libkn_KByte);
  libkn_KByte (*getNonNullValueOfByte)(libkn_kref_kotlin_Byte);
  libkn_kref_kotlin_Short (*createNullableShort)(libkn_KShort);
  libkn_KShort (*getNonNullValueOfShort)(libkn_kref_kotlin_Short);
  libkn_kref_kotlin_Int (*createNullableInt)(libkn_KInt);
  libkn_KInt (*getNonNullValueOfInt)(libkn_kref_kotlin_Int);
  libkn_kref_kotlin_Long (*createNullableLong)(libkn_KLong);
  libkn_KLong (*getNonNullValueOfLong)(libkn_kref_kotlin_Long);
  libkn_kref_kotlin_Float (*createNullableFloat)(libkn_KFloat);
  libkn_KFloat (*getNonNullValueOfFloat)(libkn_kref_kotlin_Float);
  libkn_kref_kotlin_Double (*createNullableDouble)(libkn_KDouble);
  libkn_KDouble (*getNonNullValueOfDouble)(libkn_kref_kotlin_Double);
  libkn_kref_kotlin_Char (*createNullableChar)(libkn_KChar);
  libkn_KChar (*getNonNullValueOfChar)(libkn_kref_kotlin_Char);
  libkn_kref_kotlin_Boolean (*createNullableBoolean)(libkn_KBoolean);
  libkn_KBoolean (*getNonNullValueOfBoolean)(libkn_kref_kotlin_Boolean);
  libkn_kref_kotlin_Unit (*createNullableUnit)(void);
  libkn_kref_kotlin_UByte (*createNullableUByte)(libkn_KUByte);
  libkn_KUByte (*getNonNullValueOfUByte)(libkn_kref_kotlin_UByte);
  libkn_kref_kotlin_UShort (*createNullableUShort)(libkn_KUShort);
  libkn_KUShort (*getNonNullValueOfUShort)(libkn_kref_kotlin_UShort);
  libkn_kref_kotlin_UInt (*createNullableUInt)(libkn_KUInt);
  libkn_KUInt (*getNonNullValueOfUInt)(libkn_kref_kotlin_UInt);
  libkn_kref_kotlin_ULong (*createNullableULong)(libkn_KULong);
  libkn_KULong (*getNonNullValueOfULong)(libkn_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            struct {
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_array$stableprop_getter)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_drawable$stableprop_getter)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_font$stableprop_getter)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_plurals$stableprop_getter)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_string$stableprop_getter)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_array$stableprop_getter_)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_drawable$stableprop_getter_)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_font$stableprop_getter_)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_plurals$stableprop_getter_)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_string$stableprop_getter_)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_array$stableprop_getter__)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_drawable$stableprop_getter__)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_font$stableprop_getter__)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_plurals$stableprop_getter__)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_string$stableprop_getter__)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_array$stableprop_getter___)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_drawable$stableprop_getter___)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_font$stableprop_getter___)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_plurals$stableprop_getter___)();
              libkn_KInt (*tianqu_composeapp_generated_resources_Res_string$stableprop_getter___)();
            } resources;
          } generated;
        } composeapp;
      } tianqu;
      struct {
        struct {
          struct {
            struct {
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_router_generated_GlobalRouteAggregator (*_instance)();
                libkn_kref_kotlin_collections_List (*get_routers)(libkn_kref_shijing_tianqu_router_generated_GlobalRouteAggregator thiz);
                libkn_kref_kotlin_collections_Map (*get_services)(libkn_kref_shijing_tianqu_router_generated_GlobalRouteAggregator thiz);
              } GlobalRouteAggregator;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_router_generated_RouterRegistry_ComposeApp (*_instance)();
                libkn_kref_kotlin_collections_List (*get_routers)(libkn_kref_shijing_tianqu_router_generated_RouterRegistry_ComposeApp thiz);
              } RouterRegistry_ComposeApp;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_router_generated_ServiceRegistry_ComposeApp (*_instance)();
                libkn_kref_kotlin_collections_Map (*get_services)(libkn_kref_shijing_tianqu_router_generated_ServiceRegistry_ComposeApp thiz);
              } ServiceRegistry_ComposeApp;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp (*_instance)();
                libkn_kref_kotlin_collections_Map (*get_transitions)(libkn_kref_shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp thiz);
              } TransitionStrategyRegistry_ComposeApp;
              libkn_KInt (*shijing_tianqu_router_generated_GlobalRouteAggregator$stableprop_getter)();
              libkn_KInt (*shijing_tianqu_router_generated_RouterRegistry_ComposeApp$stableprop_getter)();
              libkn_KInt (*shijing_tianqu_router_generated_ServiceRegistry_ComposeApp$stableprop_getter)();
              libkn_KInt (*shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp$stableprop_getter)();
              libkn_KInt (*shijing_tianqu_router_generated_GlobalRouteAggregator$stableprop_getter_)();
              libkn_KInt (*shijing_tianqu_router_generated_RouterRegistry_ComposeApp$stableprop_getter_)();
              libkn_KInt (*shijing_tianqu_router_generated_ServiceRegistry_ComposeApp$stableprop_getter_)();
              libkn_KInt (*shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp$stableprop_getter_)();
              libkn_KInt (*shijing_tianqu_router_generated_GlobalRouteAggregator$stableprop_getter__)();
              libkn_KInt (*shijing_tianqu_router_generated_RouterRegistry_ComposeApp$stableprop_getter__)();
              libkn_KInt (*shijing_tianqu_router_generated_ServiceRegistry_ComposeApp$stableprop_getter__)();
              libkn_KInt (*shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp$stableprop_getter__)();
              libkn_KInt (*shijing_tianqu_router_generated_GlobalRouteAggregator$stableprop_getter___)();
              libkn_KInt (*shijing_tianqu_router_generated_RouterRegistry_ComposeApp$stableprop_getter___)();
              libkn_KInt (*shijing_tianqu_router_generated_ServiceRegistry_ComposeApp$stableprop_getter___)();
              libkn_KInt (*shijing_tianqu_router_generated_TransitionStrategyRegistry_ComposeApp$stableprop_getter___)();
            } generated;
          } router;
          struct {
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_RotateScaleTransitionStrategy (*RotateScaleTransitionStrategy)();
              libkn_kref_androidx_compose_animation_EnterTransition (*getEnterTransition)(libkn_kref_shijing_tianqu_screens_RotateScaleTransitionStrategy thiz, libkn_kref_androidx_compose_animation_AnimatedContentTransitionScope scope, libkn_kref_shijing_tianqu_runtime_StackEntry initial, libkn_kref_shijing_tianqu_runtime_StackEntry target, libkn_KBoolean isPop);
              libkn_kref_androidx_compose_animation_ExitTransition (*getExitTransition)(libkn_kref_shijing_tianqu_screens_RotateScaleTransitionStrategy thiz, libkn_kref_androidx_compose_animation_AnimatedContentTransitionScope scope, libkn_kref_shijing_tianqu_runtime_StackEntry initial, libkn_kref_shijing_tianqu_runtime_StackEntry target, libkn_KBoolean isPop);
            } RotateScaleTransitionStrategy;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_UserDetailData (*UserDetailData)(const char* name, const char* desc);
              const char* (*get_desc)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
              const char* (*get_name)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
              const char* (*component1)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
              const char* (*component2)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
              libkn_kref_shijing_tianqu_screens_UserDetailData (*copy)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz, const char* name, const char* desc);
              libkn_KBoolean (*equals)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz, libkn_kref_kotlin_Any other);
              libkn_KInt (*hashCode)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
              const char* (*toString)(libkn_kref_shijing_tianqu_screens_UserDetailData thiz);
            } UserDetailData;
            struct {
              struct {
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading (*_instance)();
                } Loading;
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success (*Success)(libkn_kref_shijing_tianqu_screens_UserDetailData data);
                  libkn_kref_shijing_tianqu_screens_UserDetailData (*get_data)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz);
                  libkn_kref_shijing_tianqu_screens_UserDetailData (*component1)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz);
                  libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success (*copy)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz, libkn_kref_shijing_tianqu_screens_UserDetailData data);
                  libkn_KBoolean (*equals)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz, libkn_kref_kotlin_Any other);
                  libkn_KInt (*hashCode)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz);
                  const char* (*toString)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success thiz);
                } Success;
                struct {
                  libkn_KType* (*_type)(void);
                  libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error (*Error)(const char* message);
                  const char* (*get_message)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz);
                  const char* (*component1)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz);
                  libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error (*copy)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz, const char* message);
                  libkn_KBoolean (*equals)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz, libkn_kref_kotlin_Any other);
                  libkn_KInt (*hashCode)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz);
                  const char* (*toString)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error thiz);
                } Error;
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState (*UiState)();
              } UiState;
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel (*DemoPreloadViewModel)();
              libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel_UiState (*get_uiState)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel thiz);
              void (*onPreloadResult)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel thiz, libkn_kref_kotlin_Any result);
              void (*retry)(libkn_kref_shijing_tianqu_screens_DemoPreloadViewModel thiz);
            } DemoPreloadViewModel;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_UserDetailPreloader (*UserDetailPreloader)();
            } UserDetailPreloader;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_CounterViewModel (*CounterViewModel)();
              libkn_kref_kotlinx_coroutines_flow_StateFlow (*get_count)(libkn_kref_shijing_tianqu_screens_CounterViewModel thiz);
              void (*increment)(libkn_kref_shijing_tianqu_screens_CounterViewModel thiz);
              void (*onCleared)(libkn_kref_shijing_tianqu_screens_CounterViewModel thiz);
            } CounterViewModel;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_DynamicFeatureGuard (*DynamicFeatureGuard)();
              libkn_KBoolean (*matches)(libkn_kref_shijing_tianqu_screens_DynamicFeatureGuard thiz, libkn_kref_shijing_tianqu_router_RouterContext context);
              void (*onNavigatorReady)(libkn_kref_shijing_tianqu_screens_DynamicFeatureGuard thiz, libkn_kref_shijing_tianqu_runtime_Navigator navigator);
            } DynamicFeatureGuard;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_UserProfile (*UserProfile)(const char* name, libkn_KInt age, libkn_KBoolean isVip);
              libkn_KInt (*get_age)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              libkn_KBoolean (*get_isVip)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              const char* (*get_name)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              const char* (*component1)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              libkn_KInt (*component2)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              libkn_KBoolean (*component3)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              libkn_kref_shijing_tianqu_screens_UserProfile (*copy)(libkn_kref_shijing_tianqu_screens_UserProfile thiz, const char* name, libkn_KInt age, libkn_KBoolean isVip);
              libkn_KBoolean (*equals)(libkn_kref_shijing_tianqu_screens_UserProfile thiz, libkn_kref_kotlin_Any other);
              libkn_KInt (*hashCode)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
              const char* (*toString)(libkn_kref_shijing_tianqu_screens_UserProfile thiz);
            } UserProfile;
            struct {
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer (*_instance)();
                libkn_kref_kotlinx_serialization_descriptors_SerialDescriptor (*get_descriptor)(libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer thiz);
                libkn_kref_kotlin_Array (*childSerializers)(libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer thiz);
                libkn_kref_shijing_tianqu_screens_UserDetailArgs (*deserialize)(libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer thiz, libkn_kref_kotlinx_serialization_encoding_Decoder decoder);
                void (*serialize)(libkn_kref_shijing_tianqu_screens_UserDetailArgs_$serializer thiz, libkn_kref_kotlinx_serialization_encoding_Encoder encoder, libkn_kref_shijing_tianqu_screens_UserDetailArgs value);
              } $serializer;
              struct {
                libkn_KType* (*_type)(void);
                libkn_kref_shijing_tianqu_screens_UserDetailArgs_Companion (*_instance)();
                libkn_kref_kotlinx_serialization_KSerializer (*serializer)(libkn_kref_shijing_tianqu_screens_UserDetailArgs_Companion thiz);
              } Companion;
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_screens_UserDetailArgs (*UserDetailArgs)(libkn_KLong userId, const char* username, libkn_KBoolean isVip, libkn_kref_kotlin_collections_List scores);
              libkn_KBoolean (*get_isVip)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_kref_kotlin_collections_List (*get_scores)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_KLong (*get_userId)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              const char* (*get_username)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_KLong (*component1)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              const char* (*component2)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_KBoolean (*component3)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_kref_kotlin_collections_List (*component4)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              libkn_kref_shijing_tianqu_screens_UserDetailArgs (*copy)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz, libkn_KLong userId, const char* username, libkn_KBoolean isVip, libkn_kref_kotlin_collections_List scores);
              libkn_KBoolean (*equals)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz, libkn_kref_kotlin_Any other);
              libkn_KInt (*hashCode)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
              const char* (*toString)(libkn_kref_shijing_tianqu_screens_UserDetailArgs thiz);
            } UserDetailArgs;
            void (*DemoAnimScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter)();
            void (*DemoDialogScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_)();
            void (*DemoPreloadScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter__)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter__)();
            void (*DemoViewModelScreen)();
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter___)();
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter____)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter____)();
            void (*DynamicFeatureScreen)();
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_____)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_____)();
            void (*FavScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter______)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter______)();
            void (*HomeScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_______)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_______)();
            void (*MainTabScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter________)();
            void (*ProfileScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_________)();
            void (*SettingsScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter__________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter__________)();
            void (*SharedElementDemoScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter___________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter___________)();
            void (*TestSingleTaskMiddleScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter____________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter____________)();
            void (*TestSingleTaskScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_____________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_____________)();
            void (*TestSingleTopScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter______________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter______________)();
            void (*TypeSafeScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter_______________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter_______________)();
            void (*UserScreen)(libkn_kref_shijing_tianqu_router_RouterContext context);
            libkn_KInt (*shijing_tianqu_screens_CounterViewModel$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Error$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Loading$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DemoPreloadViewModel_UiState_Success$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_DynamicFeatureGuard$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_RotateScaleTransitionStrategy$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailArgs_$serializer$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailData$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_UserDetailPreloader$stableprop_getter________________)();
            libkn_KInt (*shijing_tianqu_screens_UserProfile$stableprop_getter________________)();
          } screens;
          struct {
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_services_AnalyticsServiceImpl (*AnalyticsServiceImpl)();
              void (*trackEvent)(libkn_kref_shijing_tianqu_services_AnalyticsServiceImpl thiz, const char* eventName);
            } AnalyticsServiceImpl;
            struct {
              libkn_KType* (*_type)(void);
              const char* (*getUserName)(libkn_kref_shijing_tianqu_services_UserService thiz);
              libkn_KBoolean (*isUserLoggedIn)(libkn_kref_shijing_tianqu_services_UserService thiz);
            } UserService;
            struct {
              libkn_KType* (*_type)(void);
              libkn_kref_shijing_tianqu_services_UserServiceImpl (*UserServiceImpl)();
              const char* (*getUserName)(libkn_kref_shijing_tianqu_services_UserServiceImpl thiz);
              libkn_KBoolean (*isUserLoggedIn)(libkn_kref_shijing_tianqu_services_UserServiceImpl thiz);
            } UserServiceImpl;
            libkn_KInt (*shijing_tianqu_services_AnalyticsServiceImpl$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_services_UserServiceImpl$stableprop_getter)();
            libkn_KInt (*shijing_tianqu_services_AnalyticsServiceImpl$stableprop_getter_)();
            libkn_KInt (*shijing_tianqu_services_UserServiceImpl$stableprop_getter_)();
          } services;
          struct {
            libkn_KType* (*_type)(void);
            libkn_kref_shijing_tianqu_Greeting (*Greeting)();
            const char* (*greet)(libkn_kref_shijing_tianqu_Greeting thiz);
          } Greeting;
          struct {
            libkn_KType* (*_type)(void);
            const char* (*get_name)(libkn_kref_shijing_tianqu_Platform thiz);
          } Platform;
          struct {
            libkn_KType* (*_type)(void);
            libkn_kref_shijing_tianqu_OhosPlatform (*OhosPlatform)();
            const char* (*get_name)(libkn_kref_shijing_tianqu_OhosPlatform thiz);
          } OhosPlatform;
          void (*App)();
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter)();
          libkn_kref_shijing_tianqu_runtime_Navigator (*rememberAppTianQuState)(libkn_kref_kotlin_Function1 block);
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter_)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter_)();
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter__)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter__)();
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter___)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter___)();
          void* (*MainArkUIViewController_)(void* env);
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter____)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter____)();
          libkn_kref_shijing_tianqu_Platform (*getPlatform)();
          libkn_KInt (*shijing_tianqu_Greeting$stableprop_getter_____)();
          libkn_KInt (*shijing_tianqu_OhosPlatform$stableprop_getter_____)();
        } tianqu;
      } shijing;
      struct {
        struct {
          struct {
            struct {
              struct {
                void (*InitJsRenderNodeContext_)(void* env, void* nodeConstructor, void* statusModifyConstructor, libkn_KDouble ratio, libkn_KBoolean fixed);
                void (*_Export_ArkUIViewController_aboutToAppear)(void* controllerRef);
                void (*_Export_ArkUIViewController_aboutToDisappear)(void* controllerRef);
                void (*_Export_ArkUIViewController_cancelSyncRefresh)(void* controllerRef, libkn_KInt refreshId);
                void (*_Export_ArkUIViewController_dispatchHoverEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_dispatchMouseEvent)(void* controllerRef);
                libkn_KBoolean (*_Export_ArkUIViewController_dispatchTouchEvent)(void* controllerRef, void* nativeTouchEvent, libkn_KBoolean ignoreInteropView);
                void (*_Export_ArkUIViewController_draw)(void* controllerRef, void* canvas);
                libkn_KInt (*_Export_ArkUIViewController_findNodeIdAt)(void* controllerRef, libkn_KFloat x, libkn_KFloat y);
                const char* (*_Export_ArkUIViewController_getId)(void* controllerRef);
                void* (*_Export_ArkUIViewController_getJsNode)(void* controllerRef);
                libkn_KInt (*_Export_ArkUIViewController_getRendererTypeId)();
                void* (*_Export_ArkUIViewController_getXComponentRender)(void* controllerRef);
                void (*_Export_ArkUIViewController_initFusionRendererNode)(void* controllerRef, libkn_KBoolean enableCApi, void* rootContent, void* frameMgr);
                void (*_Export_ArkUIViewController_keyboardWillHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_keyboardWillShow)(void* controllerRef, libkn_KFloat keyboardHeight);
                libkn_KBoolean (*_Export_ArkUIViewController_onBackPress)(void* controllerRef);
                void (*_Export_ArkUIViewController_onConfigurationUpdate)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFinalize)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFocusEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_onFrame)(void* controllerRef, libkn_KLong timestamp, libkn_KLong targetTimestamp);
                void (*_Export_ArkUIViewController_onIdle)(void* controllerRef, libkn_KLong timeLeft);
                void (*_Export_ArkUIViewController_onKeyEvent)(void* controllerRef);
                void (*_Export_ArkUIViewController_onPageHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_onPageShow)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceChanged)(void* controllerRef, libkn_KInt width, libkn_KInt height);
                void (*_Export_ArkUIViewController_onSurfaceCreated)(void* controllerRef, void* xcomponentPtr, libkn_KInt width, libkn_KInt height);
                void (*_Export_ArkUIViewController_onSurfaceDestroyed)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceHide)(void* controllerRef);
                void (*_Export_ArkUIViewController_onSurfaceShow)(void* controllerRef);
                libkn_KInt (*_Export_ArkUIViewController_requestSyncRefresh)(void* controllerRef);
                const char* (*_Export_ArkUIViewController_sendMessage)(void* controllerRef, const char* type, const char* message);
                void (*_Export_ArkUIViewController_setContext)(void* controllerRef, void* context);
                void (*_Export_ArkUIViewController_setEnv)(void* controllerRef, void* env);
                void (*_Export_ArkUIViewController_setId)(void* controllerRef, const char* id);
                void (*_Export_ArkUIViewController_setLocaleAndStringProvider)(void* controllerRef, void* provider);
                void (*_Export_ArkUIViewController_setMessenger)(void* controllerRef, void* messenger);
                void (*_Export_ArkUIViewController_setRendererBackendId)(void* controllerRef, libkn_KInt backendId);
                void (*_Export_ArkUIViewController_setRootView)(void* controllerRef, void* backRootView, void* foreRootView, void* touchableRootView);
                void (*_Export_ArkUIViewController_setUIContext)(void* controllerRef, void* uiContext);
                void (*_Export_ArkUIViewController_setXComponentRender)(void* controllerRef, void* render);
                libkn_KLong (*getCurrentTimeNanos)();
                void (*_Export_ArkUIViewInitializer_init)(void* env, void* exports);
              } arkui;
            } ui;
          } export_;
        } compose;
      } androidx;
    } root;
  } kotlin;
} libkn_ExportedSymbols;
extern libkn_ExportedSymbols* libkn_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBKN_H */
