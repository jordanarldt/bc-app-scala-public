declare global {
  interface Window {
    bcAsyncInit: () => void;
  }

  class Bigcommerce {
    public static init: (config: { onLogout: () => void }) => void;
  }
}

export function bcSDK(context: string) {
  if (typeof window === "undefined" || document.getElementById("bc-sdk-js")) return;

  const firstScript = document.getElementsByTagName("script")[0];
  const script = document.createElement("script");
  script.id = "bc-sdk-js";
  script.async = true;
  script.src = "https://cdn.bigcommerce.com/jssdk/bc-sdk.js";

  firstScript.parentNode?.insertBefore(script, firstScript);

  window.bcAsyncInit = () => {
    Bigcommerce.init({
      onLogout: () => null // any actions to perform on user logout should go here
    });
  };
}
